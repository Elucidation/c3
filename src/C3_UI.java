import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
//import java.util.Iterator;
//import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFileChooser;
//import javax.swing.JList;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.semanticweb.HermiT.Reasoner;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.reasoner.NodeSet;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerConfiguration;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;
import org.semanticweb.owlapi.reasoner.SimpleConfiguration;
import org.semanticweb.owlapi.util.DefaultPrefixManager;
import org.w3c.dom.*;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.PropertiesCredentials;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.DescribeAvailabilityZonesResult;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.Reservation;
import com.amazonaws.services.ec2.model.RunInstancesRequest;
import com.amazonaws.services.ec2.model.RunInstancesResult;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.common.IOUtils;
import net.schmizz.sshj.connection.channel.direct.Session;
import net.schmizz.sshj.connection.channel.direct.Session.Command;

import java.util.concurrent.TimeUnit;
import net.schmizz.sshj.userauth.keyprovider.FileKeyProvider;
import net.schmizz.sshj.userauth.keyprovider.KeyProvider;
import net.schmizz.sshj.userauth.keyprovider.PKCS8KeyFile;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/*
 * C3_UI.java
 *
 * Created on Apr 4, 2011, 11:06:31 PM
 */

/**
 *
 * @author Sam
 */
public class C3_UI extends javax.swing.JFrame {

    /**
     * Starts a new instance and returns a list of instance ids from it
     */
    private static Set<Instance> startNewAmazonInstance(AmazonEC2 ec2, String ami_id) {
        RunInstancesRequest request = new RunInstancesRequest(ami_id,1,1); // launch 1 instance
        request.setKeyName("demoKeypair");
        request.setInstanceType("m1.large");
        return getInstancesFromResult(ec2.runInstances(request));
    }

    /** Takes instance result info and returns instances from it as a set
     * 
     * @param instanceResult set of running instances
     * @return
     */
    private static Set<Instance> getInstancesFromResult(RunInstancesResult instanceResult) {
        Reservation reservationNewInstance = instanceResult.getReservation();
        Set<Instance> newInstances = new HashSet<Instance>();
        newInstances.addAll(reservationNewInstance.getInstances()); // only 1 in this case
        return newInstances;
    }

    private static Set<Instance> getCurrentInstances(AmazonEC2 ec2) {
        DescribeInstancesResult describeInstancesRequest = ec2.describeInstances();

        List<Reservation> reservations = describeInstancesRequest.getReservations();
        Set<Instance> newInstances = new HashSet<Instance>();

        for (Reservation reservation : reservations) {
            newInstances.addAll(reservation.getInstances());
        }
        return newInstances;
    }
    /** Read a File to a string
     * Modified from http://stackoverflow.com/questions/326390/how-to-create-a-java-string-from-the-contents-of-a-file
     *
     * @param path
     * @return
     * @throws IOException
     */
    private static String readFile(String path) throws IOException {
      FileInputStream stream = new FileInputStream(new File(path));
      try {
        FileChannel fc = stream.getChannel();
        MappedByteBuffer bb = fc.map(FileChannel.MapMode.READ_ONLY, 0, fc.size());
        /* Instead of using default, pass in a decoder. */
        return Charset.defaultCharset().decode(bb).toString();
      }
      finally {
        stream.close();
      }
    }

    private void testSSH() throws IOException {
        final SSHClient ssh = new SSHClient();

        ssh.addHostKeyVerifier("cf:0c:bf:1c:07:5e:83:0b:e1:9a:4f:0f:86:46:37:cd");

        ssh.loadKnownHosts();

        //ssh.connect( InetAddress.getByName());
        ssh.connect("ec2-50-17-153-118.compute-1.amazonaws.com");
        try {
            FileKeyProvider kp = new PKCS8KeyFile();
            kp.init(new File("D:/CollegeHW/CS Research/Keys/demo.pem"));
            ssh.authPublickey("root",kp);
            final Session session = ssh.startSession();
            try {
                final Command cmd = session.exec("ping -c 1 google.com");
                System.out.print(cmd.getOutputAsString());
                cmd.join(5, TimeUnit.SECONDS);
                System.out.println("\n** exit status: " + cmd.getExitStatus());
            } finally {
                session.close();
            }

        } finally {
            ssh.disconnect();
        }
    }

    private void updateTemplateOnScreen() {
        configurationScriptTextArea.setText(configTemplate);
    }

    /** Fills out values in template
     * 
     * @param caseName
     * @param compset
     * @param grid
     * @param machine
     */
    private void fillTemplate(String caseName, String compset, String grid, String machine) {
        configTemplate = baseTemplate;
        configTemplate = configTemplate.replaceAll("<casename>", caseName);
        configTemplate = configTemplate.replaceAll("<compset>", compset);
        configTemplate = configTemplate.replaceAll("<grid>", grid);
        configTemplate = configTemplate.replaceAll("<machine>", machine);
    }
    private void initTemplate() {
        try {
            baseTemplate = readFile(templateLoc);
            configTemplate = baseTemplate;

        } catch (IOException ex) {
            System.out.println("Couldn't read template file "+templateLoc);
            Logger.getLogger(C3_UI.class.getName()).log(Level.SEVERE, null, ex);
        }
        
    }
    /** Creates new form C3_UI */
    public C3_UI() {
        System.out.println("-------------------");
        System.out.println("Testing Amazon");
        try {
            testAmazon();
        }
        catch (Exception e)
        {
            
            System.out.println("Couldn't start Amazon AWS services, Issue is probably Security Credentials");
            System.out.println("Exception "+e.getClass().toString()+": " + e.getMessage());
        }
        System.out.println("Done testing Amazon");

        System.out.println("-------------------");

        System.out.println("Loading template");
        initTemplate();
        System.out.println("Finished loading template");

        System.out.println("-------------------");

        System.out.println("Testing SSH");
        try {
            testSSH();
        } catch (IOException ex) {
            Logger.getLogger(C3_UI.class.getName()).log(Level.SEVERE, null, ex);
        }
        System.out.println("Finished testing SSH");

        System.out.println("-------------------");

        System.out.println("Setting up OWL ontology & Reasoner");
        setupOWL();
        System.out.println("Done setting up OWL ontology & Reasoner");

        System.out.println("-------------------");

        System.out.println("Starting up GUI...");
        initComponents();

        updateTemplateOnScreen();
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jLabel14 = new javax.swing.JLabel();
        jTabbedPane1 = new javax.swing.JTabbedPane();
        jPanel6 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        compsetList = new javax.swing.JList();
        jPanel4 = new javax.swing.JPanel();
        jLabel2 = new javax.swing.JLabel();
        jLabel9 = new javax.swing.JLabel();
        jLabel10 = new javax.swing.JLabel();
        jScrollPane2 = new javax.swing.JScrollPane();
        jList2 = new javax.swing.JList();
        jScrollPane4 = new javax.swing.JScrollPane();
        jList4 = new javax.swing.JList();
        jScrollPane5 = new javax.swing.JScrollPane();
        compsetTypeList = new javax.swing.JList();
        jLabel24 = new javax.swing.JLabel();
        jComboBox1 = new javax.swing.JComboBox();
        jLabel25 = new javax.swing.JLabel();
        jComboBox2 = new javax.swing.JComboBox();
        jLabel26 = new javax.swing.JLabel();
        jLabel27 = new javax.swing.JLabel();
        jComboBox3 = new javax.swing.JComboBox();
        jComboBox4 = new javax.swing.JComboBox();
        jPanel9 = new javax.swing.JPanel();
        jPanel5 = new javax.swing.JPanel();
        jPanel11 = new javax.swing.JPanel();
        jScrollPane8 = new javax.swing.JScrollPane();
        jList8 = new javax.swing.JList();
        jLabel12 = new javax.swing.JLabel();
        jLabel19 = new javax.swing.JLabel();
        jScrollPane10 = new javax.swing.JScrollPane();
        jList9 = new javax.swing.JList();
        jButton13 = new javax.swing.JButton();
        jPanel13 = new javax.swing.JPanel();
        jScrollPane13 = new javax.swing.JScrollPane();
        jList12 = new javax.swing.JList();
        jLabel22 = new javax.swing.JLabel();
        jLabel23 = new javax.swing.JLabel();
        jScrollPane14 = new javax.swing.JScrollPane();
        jList13 = new javax.swing.JList();
        jPanel14 = new javax.swing.JPanel();
        jScrollPane15 = new javax.swing.JScrollPane();
        jList14 = new javax.swing.JList();
        jLabel28 = new javax.swing.JLabel();
        jLabel29 = new javax.swing.JLabel();
        jScrollPane16 = new javax.swing.JScrollPane();
        jList15 = new javax.swing.JList();
        jPanel15 = new javax.swing.JPanel();
        jScrollPane17 = new javax.swing.JScrollPane();
        jList16 = new javax.swing.JList();
        jLabel30 = new javax.swing.JLabel();
        jLabel31 = new javax.swing.JLabel();
        jScrollPane18 = new javax.swing.JScrollPane();
        jList17 = new javax.swing.JList();
        jLabel3 = new javax.swing.JLabel();
        jScrollPane3 = new javax.swing.JScrollPane();
        gridList = new javax.swing.JList();
        jPanel7 = new javax.swing.JPanel();
        jPanel2 = new javax.swing.JPanel();
        jLabel4 = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        jLabel6 = new javax.swing.JLabel();
        jLabel7 = new javax.swing.JLabel();
        keypairNameField = new javax.swing.JTextField();
        keypairLocField = new javax.swing.JTextField();
        privatekeyLocField = new javax.swing.JTextField();
        certificateLocField = new javax.swing.JTextField();
        keypairBrowseButton = new javax.swing.JButton();
        privatekeyBrowseButton = new javax.swing.JButton();
        certificateBrowseButton = new javax.swing.JButton();
        jButton10 = new javax.swing.JButton();
        jLabel13 = new javax.swing.JLabel();
        startInstanceButton = new javax.swing.JButton();
        loadConfigXML = new javax.swing.JButton();
        jScrollPane9 = new javax.swing.JScrollPane();
        configurationScriptTextArea = new javax.swing.JTextArea();
        jLabel15 = new javax.swing.JLabel();
        jPanel8 = new javax.swing.JPanel();
        jLabel16 = new javax.swing.JLabel();
        compsetField = new javax.swing.JTextField();
        jLabel17 = new javax.swing.JLabel();
        gridField = new javax.swing.JTextField();
        jLabel18 = new javax.swing.JLabel();
        casenameField = new javax.swing.JTextField();
        saveConfigButton = new javax.swing.JButton();
        updateConfigScript = new javax.swing.JButton();
        ExitButton = new javax.swing.JButton();
        saveConfigToXMLButton = new javax.swing.JButton();
        jButton2 = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("Cloud Climate Configurator v2");
        setForeground(java.awt.Color.white);
        setName("C3_mainframe"); // NOI18N

        jLabel14.setText("To Create a new Configuration, choose an available Component Set and Grid (narrow down with Specific Features), then go to the Cloud tab");

        jLabel1.setText("Available Compsets");

        compsetList.setModel(new javax.swing.AbstractListModel() {
            String[] strings = getOWLCompset("NamedCompSets");//{ "Not Selected", "A_PRESENT_DAY", "A_GLC", "B_2000", "B_2000_CN", "B_1850_CAM5", "B_1850", "B_1850_CN", "B_2000_CN_CHEM", "B_1850_CN_CHEM", "B_1850_RAMPCO2_CN", "B_18502000", "B_18502000_CN", "B_18502000_CN_CHEM", "B_18502000_CAM5", "B_2000_GLC", "B_2000_TROP_MOZART", "B_1850_WACCM", "B_1850_WACCM_CN", "B_18502000_WACCM_CN", "B_1850_BGCBPRP", "B_1850_BGCBDRD", "B_18502000_BGCBPRP", "B_18502000_BGCBDRD", "C_NORMAL_YEAR_ECOSYS", "C_NORMAL_YEAR", "D_NORMAL_YEAR", "E_2000", "E_2000_GLC", "E_1850_CN", "E_1850_CAM5", "F_AMIP_CN", "F_AMIP_CAM5", "F_1850", "F_1850_CAM5", "F_2000", "F_2000_CAM5", "F_2000_CN", "F_18502000_CN", "F_2000_GLC", "F_1850_CN_CHEM", "F_1850_WACCM", "F_1850_WACCM", "F_2000_WACCM", "G_1850_ECOSYS", "G_NORMAL_YEAR", "H_PRESENT_DAY", "I_2000", "I_1850", "I_2000_GLC", "I_19482004", "I_18502000", "I_2000_CN", "I_1850_CN", "I_19482004_CN", "I_18502000_CN", "S_PRESENT_DAY", "X_PRESENT_DAY", "XG_PRESENT_DAY" };
            public int getSize() { return strings.length; }
            public Object getElementAt(int i) { return strings[i]; }
        });
        compsetList.addListSelectionListener(new javax.swing.event.ListSelectionListener() {
            public void valueChanged(javax.swing.event.ListSelectionEvent evt) {
                compsetListValueChanged(evt);
            }
        });
        jScrollPane1.setViewportView(compsetList);

        jPanel4.setBorder(javax.swing.BorderFactory.createTitledBorder("Specific Features"));

        jLabel2.setText("Type");

        jLabel9.setText("Date");

        jLabel10.setText("Extras");

        jList2.setModel(new javax.swing.AbstractListModel() {
            String[] strings = { "any", "LC", "CN", "CHEM", "RAMPCO2", "WACCM" };
            public int getSize() { return strings.length; }
            public Object getElementAt(int i) { return strings[i]; }
        });
        jScrollPane2.setViewportView(jList2);

        jList4.setModel(new javax.swing.AbstractListModel() {
            String[] strings = { "any", "1850", "2000", "PRESENT_DAY" };
            public int getSize() { return strings.length; }
            public Object getElementAt(int i) { return strings[i]; }
        });
        jScrollPane4.setViewportView(jList4);

        compsetTypeList.setModel(new javax.swing.AbstractListModel() {
            String[] strings = { "any", "A - All DATA components with stub glc (used primarily for testing)", "B - FULLY ACTIVE components with stub glc", "C - POP active with data atm, lnd(runoff), and ice plus stub glc", "D - CICE active with data atm and ocean plus stub land and glc", "E - CAM, CLM, and CICE active with data ocean (som mode) plus stub glc", "F - CAM, CLM, and CICE(prescribed mode) active with data ocean (sstdata mode) plus stub glc", "G - POP and CICE active with data atm and lnd(runoff) plus stub glc", "H - POP and CICE active with data atm and stub land and glc", "I - CLM active with data atm and stub ice, ocean, and glc", "S - All STUB components (used for testing only)", "X - All DEAD components except for stub glc (used for testing only)" };
            public int getSize() { return strings.length; }
            public Object getElementAt(int i) { return strings[i]; }
        });
        compsetTypeList.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                compsetTypeListMouseClicked(evt);
            }
        });
        compsetTypeList.addListSelectionListener(new javax.swing.event.ListSelectionListener() {
            public void valueChanged(javax.swing.event.ListSelectionEvent evt) {
                compsetTypeListValueChanged(evt);
            }
        });
        compsetTypeList.addPropertyChangeListener(new java.beans.PropertyChangeListener() {
            public void propertyChange(java.beans.PropertyChangeEvent evt) {
                compsetTypeListPropertyChange(evt);
            }
        });
        jScrollPane5.setViewportView(compsetTypeList);

        jLabel24.setText("Atmosphere");

        jComboBox1.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Alive", "Data", "Stub", "Dead" }));

        jLabel25.setText("Land");

        jComboBox2.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Alive", "Data", "Stub", "Dead" }));

        jLabel26.setText("Ocean");

        jLabel27.setText("Ice");

        jComboBox3.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Alive", "Data", "Stub", "Dead" }));

        jComboBox4.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Alive", "Data", "Stub", "Dead" }));

        javax.swing.GroupLayout jPanel4Layout = new javax.swing.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel4Layout.createSequentialGroup()
                        .addContainerGap()
                        .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jScrollPane5, javax.swing.GroupLayout.PREFERRED_SIZE, 325, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel2)))
                    .addGroup(jPanel4Layout.createSequentialGroup()
                        .addGap(95, 95, 95)
                        .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel24)
                            .addComponent(jLabel25)
                            .addComponent(jLabel26)
                            .addComponent(jLabel27))
                        .addGap(19, 19, 19)
                        .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(jComboBox2, javax.swing.GroupLayout.PREFERRED_SIZE, 63, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jComboBox1, javax.swing.GroupLayout.PREFERRED_SIZE, 76, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jComboBox3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jComboBox4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))))
                .addGap(18, 18, 18)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel9)
                    .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                        .addComponent(jLabel10)
                        .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 149, Short.MAX_VALUE)
                        .addComponent(jScrollPane4)))
                .addGap(46, 46, 46))
        );

        jPanel4Layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {jComboBox1, jComboBox2, jComboBox3, jComboBox4});

        jPanel4Layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {jLabel24, jLabel25, jLabel26, jLabel27});

        jPanel4Layout.setVerticalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel2)
                    .addComponent(jLabel9))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel4Layout.createSequentialGroup()
                        .addComponent(jScrollPane4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(11, 11, 11)
                        .addComponent(jLabel10)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel4Layout.createSequentialGroup()
                        .addComponent(jScrollPane5, javax.swing.GroupLayout.PREFERRED_SIZE, 222, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jComboBox1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel24))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel25, javax.swing.GroupLayout.PREFERRED_SIZE, 14, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jComboBox2, javax.swing.GroupLayout.PREFERRED_SIZE, 20, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel26, javax.swing.GroupLayout.PREFERRED_SIZE, 14, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jComboBox3, javax.swing.GroupLayout.PREFERRED_SIZE, 20, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel27, javax.swing.GroupLayout.PREFERRED_SIZE, 14, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jComboBox4, javax.swing.GroupLayout.PREFERRED_SIZE, 20, javax.swing.GroupLayout.PREFERRED_SIZE))))
                .addContainerGap(113, Short.MAX_VALUE))
        );

        jPanel4Layout.linkSize(javax.swing.SwingConstants.VERTICAL, new java.awt.Component[] {jComboBox1, jComboBox2, jComboBox3, jComboBox4, jLabel24, jLabel25, jLabel26, jLabel27});

        javax.swing.GroupLayout jPanel6Layout = new javax.swing.GroupLayout(jPanel6);
        jPanel6.setLayout(jPanel6Layout);
        jPanel6Layout.setHorizontalGroup(
            jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel6Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel4, javax.swing.GroupLayout.PREFERRED_SIZE, 560, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel1)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 360, Short.MAX_VALUE))
                .addContainerGap())
        );
        jPanel6Layout.setVerticalGroup(
            jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel6Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jPanel4, javax.swing.GroupLayout.DEFAULT_SIZE, 498, Short.MAX_VALUE)
                    .addGroup(jPanel6Layout.createSequentialGroup()
                        .addComponent(jLabel1)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 478, Short.MAX_VALUE)))
                .addContainerGap())
        );

        jTabbedPane1.addTab("Components", jPanel6);

        jPanel5.setBorder(javax.swing.BorderFactory.createTitledBorder("Specific Features"));

        jPanel11.setBorder(javax.swing.BorderFactory.createTitledBorder("Atmosphere"));

        jList8.setModel(new javax.swing.AbstractListModel() {
            String[] strings = { "any", "pt1", "0.23x0.31", "0.47x0.63", "0.9x1.25", "1.9x2.5", "96x192", "48x96", "64x128", "10x15", "ne30np4", "128x256" };
            public int getSize() { return strings.length; }
            public Object getElementAt(int i) { return strings[i]; }
        });
        jList8.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        jScrollPane8.setViewportView(jList8);

        jLabel12.setText("Resolution");

        jLabel19.setText("Type");

        jList9.setModel(new javax.swing.AbstractListModel() {
            String[] strings = { "any", "Finite Volume", "Displaced Pole", "Point", "Spectral", "Triple Pole" };
            public int getSize() { return strings.length; }
            public Object getElementAt(int i) { return strings[i]; }
        });
        jList9.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        jScrollPane10.setViewportView(jList9);

        javax.swing.GroupLayout jPanel11Layout = new javax.swing.GroupLayout(jPanel11);
        jPanel11.setLayout(jPanel11Layout);
        jPanel11Layout.setHorizontalGroup(
            jPanel11Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel11Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel11Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane10, javax.swing.GroupLayout.PREFERRED_SIZE, 114, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel19))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel11Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel12)
                    .addComponent(jScrollPane8, javax.swing.GroupLayout.PREFERRED_SIZE, 116, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel11Layout.setVerticalGroup(
            jPanel11Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel11Layout.createSequentialGroup()
                .addGroup(jPanel11Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel19)
                    .addComponent(jLabel12, javax.swing.GroupLayout.PREFERRED_SIZE, 16, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel11Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                    .addComponent(jScrollPane8, javax.swing.GroupLayout.Alignment.LEADING, 0, 0, Short.MAX_VALUE)
                    .addComponent(jScrollPane10, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 122, Short.MAX_VALUE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jButton13.setText("Create Custom Grid");

        jPanel13.setBorder(javax.swing.BorderFactory.createTitledBorder("Ocean"));

        jList12.setModel(new javax.swing.AbstractListModel() {
            String[] strings = { "any", "pt1", "0.23x0.31", "0.47x0.63", "0.9x1.25", "1.9x2.5", "96x192", "48x96", "64x128", "10x15", "ne30np4", "128x256" };
            public int getSize() { return strings.length; }
            public Object getElementAt(int i) { return strings[i]; }
        });
        jList12.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        jScrollPane13.setViewportView(jList12);

        jLabel22.setText("Resolution");

        jLabel23.setText("Type");

        jList13.setModel(new javax.swing.AbstractListModel() {
            String[] strings = { "any", "Finite Volume", "Displaced Pole", "Point", "Spectral", "Triple Pole" };
            public int getSize() { return strings.length; }
            public Object getElementAt(int i) { return strings[i]; }
        });
        jList13.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        jScrollPane14.setViewportView(jList13);

        javax.swing.GroupLayout jPanel13Layout = new javax.swing.GroupLayout(jPanel13);
        jPanel13.setLayout(jPanel13Layout);
        jPanel13Layout.setHorizontalGroup(
            jPanel13Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel13Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel13Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane14, javax.swing.GroupLayout.PREFERRED_SIZE, 114, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel23))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel13Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel22)
                    .addComponent(jScrollPane13, javax.swing.GroupLayout.PREFERRED_SIZE, 116, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel13Layout.setVerticalGroup(
            jPanel13Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel13Layout.createSequentialGroup()
                .addGroup(jPanel13Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel23)
                    .addComponent(jLabel22, javax.swing.GroupLayout.PREFERRED_SIZE, 16, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel13Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                    .addComponent(jScrollPane13, javax.swing.GroupLayout.Alignment.LEADING, 0, 0, Short.MAX_VALUE)
                    .addComponent(jScrollPane14, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 122, Short.MAX_VALUE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jPanel14.setBorder(javax.swing.BorderFactory.createTitledBorder("Land"));

        jList14.setModel(new javax.swing.AbstractListModel() {
            String[] strings = { "any", "pt1", "0.23x0.31", "0.47x0.63", "0.9x1.25", "1.9x2.5", "96x192", "48x96", "64x128", "10x15", "ne30np4", "128x256" };
            public int getSize() { return strings.length; }
            public Object getElementAt(int i) { return strings[i]; }
        });
        jList14.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        jScrollPane15.setViewportView(jList14);

        jLabel28.setText("Resolution");

        jLabel29.setText("Type");

        jList15.setModel(new javax.swing.AbstractListModel() {
            String[] strings = { "any", "Finite Volume", "Displaced Pole", "Point", "Spectral", "Triple Pole" };
            public int getSize() { return strings.length; }
            public Object getElementAt(int i) { return strings[i]; }
        });
        jList15.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        jScrollPane16.setViewportView(jList15);

        javax.swing.GroupLayout jPanel14Layout = new javax.swing.GroupLayout(jPanel14);
        jPanel14.setLayout(jPanel14Layout);
        jPanel14Layout.setHorizontalGroup(
            jPanel14Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel14Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel14Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane16, javax.swing.GroupLayout.PREFERRED_SIZE, 114, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel29))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel14Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel28)
                    .addComponent(jScrollPane15, javax.swing.GroupLayout.PREFERRED_SIZE, 116, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel14Layout.setVerticalGroup(
            jPanel14Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel14Layout.createSequentialGroup()
                .addGroup(jPanel14Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel29)
                    .addComponent(jLabel28, javax.swing.GroupLayout.PREFERRED_SIZE, 16, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel14Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                    .addComponent(jScrollPane15, javax.swing.GroupLayout.Alignment.LEADING, 0, 0, Short.MAX_VALUE)
                    .addComponent(jScrollPane16, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 122, Short.MAX_VALUE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jPanel15.setBorder(javax.swing.BorderFactory.createTitledBorder("Ice"));

        jList16.setModel(new javax.swing.AbstractListModel() {
            String[] strings = { "any", "pt1", "0.23x0.31", "0.47x0.63", "0.9x1.25", "1.9x2.5", "96x192", "48x96", "64x128", "10x15", "ne30np4", "128x256" };
            public int getSize() { return strings.length; }
            public Object getElementAt(int i) { return strings[i]; }
        });
        jList16.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        jScrollPane17.setViewportView(jList16);

        jLabel30.setText("Resolution");

        jLabel31.setText("Type");

        jList17.setModel(new javax.swing.AbstractListModel() {
            String[] strings = { "any", "Finite Volume", "Displaced Pole", "Point", "Spectral", "Triple Pole" };
            public int getSize() { return strings.length; }
            public Object getElementAt(int i) { return strings[i]; }
        });
        jList17.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        jScrollPane18.setViewportView(jList17);

        javax.swing.GroupLayout jPanel15Layout = new javax.swing.GroupLayout(jPanel15);
        jPanel15.setLayout(jPanel15Layout);
        jPanel15Layout.setHorizontalGroup(
            jPanel15Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel15Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel15Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane18, javax.swing.GroupLayout.PREFERRED_SIZE, 114, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel31))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel15Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel30)
                    .addComponent(jScrollPane17, javax.swing.GroupLayout.PREFERRED_SIZE, 116, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel15Layout.setVerticalGroup(
            jPanel15Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel15Layout.createSequentialGroup()
                .addGroup(jPanel15Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel31)
                    .addComponent(jLabel30, javax.swing.GroupLayout.PREFERRED_SIZE, 16, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel15Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                    .addComponent(jScrollPane17, javax.swing.GroupLayout.Alignment.LEADING, 0, 0, Short.MAX_VALUE)
                    .addComponent(jScrollPane18, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 122, Short.MAX_VALUE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout jPanel5Layout = new javax.swing.GroupLayout(jPanel5);
        jPanel5.setLayout(jPanel5Layout);
        jPanel5Layout.setHorizontalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel5Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jButton13)
                    .addGroup(jPanel5Layout.createSequentialGroup()
                        .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                            .addComponent(jPanel13, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jPanel11, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                            .addComponent(jPanel15, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jPanel14, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))))
                .addContainerGap())
        );
        jPanel5Layout.setVerticalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel5Layout.createSequentialGroup()
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel5Layout.createSequentialGroup()
                        .addComponent(jPanel11, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jPanel13, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel5Layout.createSequentialGroup()
                        .addComponent(jPanel14, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jPanel15, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jButton13)
                .addContainerGap(67, Short.MAX_VALUE))
        );

        jLabel3.setText("Available Grids");

        gridList.setModel(new javax.swing.AbstractListModel() {
            String[] strings = { "Not Selected", "pt1_pt1", "f02_f02", "f02_g16", "f02_t12", "f05_f05", "f05_g16", "f05_t12", "f09_f09", "f09_g16", "f19_f19", "f19_g16", "f45_f45", "f45_g37", "T62_g37", "T62_t12", "T62_g16", "T31_T31", "T31_g37", "T42_T42", "f10_f10", "ne30_f19_g16", "ne240_f02_g16", "T85_T85" };
            public int getSize() { return strings.length; }
            public Object getElementAt(int i) { return strings[i]; }
        });
        gridList.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        gridList.addListSelectionListener(new javax.swing.event.ListSelectionListener() {
            public void valueChanged(javax.swing.event.ListSelectionEvent evt) {
                gridListValueChanged(evt);
            }
        });
        jScrollPane3.setViewportView(gridList);

        javax.swing.GroupLayout jPanel9Layout = new javax.swing.GroupLayout(jPanel9);
        jPanel9.setLayout(jPanel9Layout);
        jPanel9Layout.setHorizontalGroup(
            jPanel9Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel9Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel5, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel9Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel3)
                    .addComponent(jScrollPane3, javax.swing.GroupLayout.PREFERRED_SIZE, 146, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(84, 84, 84))
        );
        jPanel9Layout.setVerticalGroup(
            jPanel9Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel9Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel9Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jPanel5, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(jPanel9Layout.createSequentialGroup()
                        .addComponent(jLabel3)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jScrollPane3, javax.swing.GroupLayout.DEFAULT_SIZE, 478, Short.MAX_VALUE)))
                .addContainerGap())
        );

        jTabbedPane1.addTab("Grids", jPanel9);

        jPanel2.setBorder(javax.swing.BorderFactory.createTitledBorder("Amazon EC2 Information"));

        jLabel4.setText("Keypair Name");

        jLabel5.setText("Keypair");

        jLabel6.setText("Private Key");

        jLabel7.setText("Certificate");

        keypairNameField.setText("<keypair name>");
        keypairNameField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                keypairNameFieldActionPerformed(evt);
            }
        });

        keypairLocField.setText("$HOME/<where your keypair is>/pk-XX.pem");

        privatekeyLocField.setText("$HOME/<where your private key is>/pk-XX.pem");

        certificateLocField.setText("$HOME/<where your certificate is>/cert-XX.pem");

        keypairBrowseButton.setText("Browse");
        keypairBrowseButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                keypairBrowseButtonActionPerformed(evt);
            }
        });

        privatekeyBrowseButton.setText("Browse");
        privatekeyBrowseButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                privatekeyBrowseButtonActionPerformed(evt);
            }
        });

        certificateBrowseButton.setText("Browse");
        certificateBrowseButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                certificateBrowseButtonActionPerformed(evt);
            }
        });

        jButton10.setText("Create New Keypair");

        jLabel13.setText("or");

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGap(10, 10, 10)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jLabel4)
                    .addComponent(jLabel6)
                    .addComponent(jLabel5)
                    .addComponent(jLabel7))
                .addGap(18, 18, 18)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel2Layout.createSequentialGroup()
                        .addComponent(keypairNameField, javax.swing.GroupLayout.DEFAULT_SIZE, 182, Short.MAX_VALUE)
                        .addGap(18, 18, 18)
                        .addComponent(jLabel13)
                        .addGap(18, 18, 18)
                        .addComponent(jButton10))
                    .addComponent(certificateLocField, javax.swing.GroupLayout.DEFAULT_SIZE, 357, Short.MAX_VALUE)
                    .addComponent(privatekeyLocField, javax.swing.GroupLayout.DEFAULT_SIZE, 357, Short.MAX_VALUE)
                    .addComponent(keypairLocField, javax.swing.GroupLayout.DEFAULT_SIZE, 357, Short.MAX_VALUE))
                .addGap(18, 18, 18)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(certificateBrowseButton)
                    .addComponent(privatekeyBrowseButton)
                    .addComponent(keypairBrowseButton))
                .addContainerGap())
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel4)
                    .addComponent(keypairNameField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jButton10)
                    .addComponent(jLabel13))
                .addGap(17, 17, 17)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel5)
                    .addComponent(keypairLocField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(keypairBrowseButton))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel6)
                    .addComponent(privatekeyLocField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(privatekeyBrowseButton))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel7)
                    .addComponent(certificateLocField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(certificateBrowseButton))
                .addContainerGap(74, Short.MAX_VALUE))
        );

        startInstanceButton.setText("Start up Instance with Current Configuration");

        loadConfigXML.setText("Load Configuration XML");
        loadConfigXML.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                loadConfigXMLActionPerformed(evt);
            }
        });

        configurationScriptTextArea.setColumns(20);
        configurationScriptTextArea.setRows(5);
        configurationScriptTextArea.setText("#!/bin/bash\n\ncd ~/ccsm4/scripts/\n\necho \"Creating new case...\"\n./create_newcase -case <casename> -res <grid> -compset <compset> -mach <machine>\ncd ~/ccsm4/scripts/<casename>/\n\n\necho \"Configuring case...\"\n./configure -case\n\n\necho \"Building case...\"\n./<casename>.<machine>.clean_build\n./<casename>.<machine>.build\n\necho \"Running simulation...\"\n./<casename>.<machine>.run\n\necho \"Run complete.\"");
        jScrollPane9.setViewportView(configurationScriptTextArea);

        jLabel15.setText("Current Configuration Script");

        jPanel8.setBorder(javax.swing.BorderFactory.createTitledBorder("Current Configuration"));

        jLabel16.setText("CompSet");

        compsetField.setText("<compset>");
        compsetField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                compsetFieldActionPerformed(evt);
            }
        });

        jLabel17.setText("Grid");

        gridField.setText("<grid>");
        gridField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                gridFieldActionPerformed(evt);
            }
        });

        jLabel18.setText("Case Name");

        casenameField.setText("<casename>");
        casenameField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                casenameFieldActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel8Layout = new javax.swing.GroupLayout(jPanel8);
        jPanel8.setLayout(jPanel8Layout);
        jPanel8Layout.setHorizontalGroup(
            jPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel8Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel16)
                    .addComponent(compsetField, javax.swing.GroupLayout.PREFERRED_SIZE, 164, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel17)
                    .addComponent(gridField, javax.swing.GroupLayout.PREFERRED_SIZE, 164, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel18)
                    .addComponent(casenameField, javax.swing.GroupLayout.PREFERRED_SIZE, 164, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(168, Short.MAX_VALUE))
        );
        jPanel8Layout.setVerticalGroup(
            jPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel8Layout.createSequentialGroup()
                .addComponent(jLabel18)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(casenameField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel16)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(compsetField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel17)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(gridField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(40, Short.MAX_VALUE))
        );

        saveConfigButton.setText("Save Configuration Script");
        saveConfigButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                saveConfigButtonActionPerformed(evt);
            }
        });

        updateConfigScript.setText("Update Configuration Script");
        updateConfigScript.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                updateConfigScriptActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel7Layout = new javax.swing.GroupLayout(jPanel7);
        jPanel7.setLayout(jPanel7Layout);
        jPanel7Layout.setHorizontalGroup(
            jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel7Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane9, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 930, Short.MAX_VALUE)
                    .addGroup(jPanel7Layout.createSequentialGroup()
                        .addGroup(jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(loadConfigXML)
                            .addComponent(jPanel8, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addGap(18, 18, 18)
                        .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(jLabel15)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel7Layout.createSequentialGroup()
                        .addComponent(updateConfigScript)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(saveConfigButton)
                        .addGap(18, 18, 18)
                        .addComponent(startInstanceButton)))
                .addContainerGap())
        );
        jPanel7Layout.setVerticalGroup(
            jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel7Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addGroup(jPanel7Layout.createSequentialGroup()
                        .addComponent(loadConfigXML)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jPanel8, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, 233, Short.MAX_VALUE))
                .addGap(11, 11, 11)
                .addComponent(jLabel15)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane9, javax.swing.GroupLayout.DEFAULT_SIZE, 205, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(startInstanceButton)
                    .addComponent(saveConfigButton)
                    .addComponent(updateConfigScript))
                .addContainerGap())
        );

        jTabbedPane1.addTab("Cloud", jPanel7);

        ExitButton.setText("Exit");
        ExitButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ExitButtonActionPerformed(evt);
            }
        });

        saveConfigToXMLButton.setText("Save Current Configuration to XML");
        saveConfigToXMLButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                saveConfigToXMLButtonActionPerformed(evt);
            }
        });

        jButton2.setText("Load Previous Configuration XML");
        jButton2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton2ActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(saveConfigToXMLButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jButton2)
                        .addGap(18, 18, 18)
                        .addComponent(ExitButton, javax.swing.GroupLayout.PREFERRED_SIZE, 61, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(layout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(jLabel14, javax.swing.GroupLayout.PREFERRED_SIZE, 795, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(jTabbedPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 955, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel14)
                .addGap(15, 15, 15)
                .addComponent(jTabbedPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 548, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(saveConfigToXMLButton)
                    .addComponent(jButton2)
                    .addComponent(ExitButton, javax.swing.GroupLayout.PREFERRED_SIZE, 23, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void certificateBrowseButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_certificateBrowseButtonActionPerformed
        int returnVal = fc.showOpenDialog(this);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            File file = fc.getSelectedFile();
            certificateLocField.setText(file.getPath());
        }
}//GEN-LAST:event_certificateBrowseButtonActionPerformed

    private void privatekeyBrowseButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_privatekeyBrowseButtonActionPerformed
        int returnVal = fc.showOpenDialog(this);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            File file = fc.getSelectedFile();
            privatekeyLocField.setText(file.getPath());
        }
}//GEN-LAST:event_privatekeyBrowseButtonActionPerformed

    private void keypairBrowseButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_keypairBrowseButtonActionPerformed
        int returnVal = fc.showOpenDialog(this);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            File file = fc.getSelectedFile();
            keypairLocField.setText(file.getPath());
        }
}//GEN-LAST:event_keypairBrowseButtonActionPerformed

    private void keypairNameFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_keypairNameFieldActionPerformed
        // TODO add your handling code here:
}//GEN-LAST:event_keypairNameFieldActionPerformed

    private void jButton2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton2ActionPerformed
        // TODO add your handling code here:
}//GEN-LAST:event_jButton2ActionPerformed

    private void compsetFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_compsetFieldActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_compsetFieldActionPerformed

    private void gridFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_gridFieldActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_gridFieldActionPerformed

    private void compsetListValueChanged(javax.swing.event.ListSelectionEvent evt) {//GEN-FIRST:event_compsetListValueChanged
        try { compsetField.setText(compsetList.getSelectedValue().toString()); } catch (Exception e) {}
    }//GEN-LAST:event_compsetListValueChanged

    private void casenameFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_casenameFieldActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_casenameFieldActionPerformed

    private void ExitButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ExitButtonActionPerformed
        // TODO add your handling code here:
        System.exit(0);
    }//GEN-LAST:event_ExitButtonActionPerformed

    private void saveConfigToXMLButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_saveConfigToXMLButtonActionPerformed
        // Take chosen options from available compset & grid
        // and create an XML config file from it
        int returnVal = fc.showSaveDialog(this);

        if (returnVal == JFileChooser.APPROVE_OPTION) {
            File file = fc.getSelectedFile();
            System.out.println("Saving to: " + file.getName());
            String c,g;
            if (compsetList.getSelectedValue()==null) { c = "B_2000"; }
            else { c = compsetList.getSelectedValue().toString(); }
            if (gridList.getSelectedValue()==null) { g = "f09_g16"; }
            else { g = gridList.getSelectedValue().toString(); }
            makeXML(file, c , g );
        }
    }//GEN-LAST:event_saveConfigToXMLButtonActionPerformed

    private void compsetTypeListValueChanged(javax.swing.event.ListSelectionEvent evt) {//GEN-FIRST:event_compsetTypeListValueChanged
        
        String c = compsetTypeList.getSelectedValue().toString();
        String[] newcompsetList = { "None Selected", "A_PRESENT_DAY", "A_GLC", "B_2000", "B_2000_CN", "B_1850_CAM5", "B_1850", "B_1850_CN", "B_2000_CN_CHEM", "B_1850_CN_CHEM", "B_1850_RAMPCO2_CN", "B_18502000", "B_18502000_CN", "B_18502000_CN_CHEM", "B_18502000_CAM5", "B_2000_GLC", "B_2000_TROP_MOZART", "B_1850_WACCM", "B_1850_WACCM_CN", "B_18502000_WACCM_CN", "B_1850_BGCBPRP", "B_1850_BGCBDRD", "B_18502000_BGCBPRP", "B_18502000_BGCBDRD", "C_NORMAL_YEAR_ECOSYS", "C_NORMAL_YEAR", "D_NORMAL_YEAR", "E_2000", "E_2000_GLC", "E_1850_CN", "E_1850_CAM5", "F_AMIP_CN", "F_AMIP_CAM5", "F_1850", "F_1850_CAM5", "F_2000", "F_2000_CAM5", "F_2000_CN", "F_18502000_CN", "F_2000_GLC", "F_1850_CN_CHEM", "F_1850_WACCM", "F_1850_WACCM", "F_2000_WACCM", "G_1850_ECOSYS", "G_NORMAL_YEAR", "H_PRESENT_DAY", "I_2000", "I_1850", "I_2000_GLC", "I_19482004", "I_18502000", "I_2000_CN", "I_1850_CN", "I_19482004_CN", "I_18502000_CN", "S_PRESENT_DAY", "X_PRESENT_DAY", "XG_PRESENT_DAY" };
        if (!lastCompsetType.equals(c)) {
            System.out.println("Getting OWL Compset subset '"+c+"' from ontology");//getOWLCompset();
            if (c.equals("any")) {
                getOWLCompset("NamedCompSets");
            } else {
                newcompsetList = getOWLCompset(c.substring(0,1)); // This can be abused since it's direct connection to list, replace in the future
            }
            /*else if (c.substring(0, 1).equals("A")){
                newcompsetList = getOWLCompset("A");
            } else if (c.substring(0, 1).equals("B")){
                newcompsetList = getOWLCompset("B");
            }*/
            lastCompsetType = compsetTypeList.getSelectedValue().toString();
            compsetList.setListData(newcompsetList);
        }
    }//GEN-LAST:event_compsetTypeListValueChanged

    private void compsetTypeListPropertyChange(java.beans.PropertyChangeEvent evt) {//GEN-FIRST:event_compsetTypeListPropertyChange
        // TODO add your handling code here:
    }//GEN-LAST:event_compsetTypeListPropertyChange

    private void compsetTypeListMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_compsetTypeListMouseClicked
        // TODO add your handling code here:
    }//GEN-LAST:event_compsetTypeListMouseClicked

    private void loadConfigXMLActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_loadConfigXMLActionPerformed
        // Load an XML file from here
        // Take the compset and grid value and replace fields with it
        int returnVal = fc.showOpenDialog(this);

        if (returnVal == JFileChooser.APPROVE_OPTION) {
            File file = fc.getSelectedFile();
            System.out.println("Loading from: " + file.getName());
            String[] comp_grid = readXML(file);
            compsetField.setText(comp_grid[0]);
            gridField.setText(comp_grid[1]);
        }
    }//GEN-LAST:event_loadConfigXMLActionPerformed

    private void updateConfigScriptActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_updateConfigScriptActionPerformed
        fillTemplate(casenameField.getText(),compsetField.getText(),gridField.getText(),"samubuntu");
        updateTemplateOnScreen();
    }//GEN-LAST:event_updateConfigScriptActionPerformed

    private void saveStringToFile(String str, File f) throws FileNotFoundException, IOException {
        System.out.println("Saving following String to file "+f.getPath()+":\n" + str);
        // Save to File
        BufferedWriter out = new BufferedWriter(new OutputStreamWriter(
                new FileOutputStream(f)));
        out.append(str);
        out.flush();
        out.close();
        System.out.println("File saved successfully!");
    }
    /**
     * Saves modified template to file
     * @param evt
     */
    private void saveConfigButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_saveConfigButtonActionPerformed
        int returnVal = fc.showSaveDialog(this);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            File file = fc.getSelectedFile();
            System.out.println("Saving to: " + file.getName());
            try {
                saveStringToFile(configurationScriptTextArea.getText(), file);
            } catch (FileNotFoundException ex) {
                Logger.getLogger(C3_UI.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IOException ex) {
                Logger.getLogger(C3_UI.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }//GEN-LAST:event_saveConfigButtonActionPerformed

    private void gridListValueChanged(javax.swing.event.ListSelectionEvent evt) {//GEN-FIRST:event_gridListValueChanged
        try {gridField.setText(gridList.getSelectedValue().toString());} catch (Exception e) {}
    }//GEN-LAST:event_gridListValueChanged

    private void makeXML(File f, String compset, String grid) {
        try {
            /////////////////////////////
            //Creating an empty XML Document

            //We need a Document
            DocumentBuilderFactory dbfac = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = dbfac.newDocumentBuilder();
            Document doc = docBuilder.newDocument();

            ////////////////////////
            //Creating the XML tree

            //create the root element and add it to the document
            Element root = doc.createElement("case");
            doc.appendChild(root);

            //create a comment and put it in the root element
            Comment comment = doc.createComment("Contains Compset & Grid");
            root.appendChild(comment);

            //create compset element, add an attribute, and add to root
            Element c = doc.createElement("compset");
            c.setAttribute("name", compset);
            root.appendChild(c);
            //create compset element, add an attribute, and add to root
            Element g = doc.createElement("grid");
            g.setAttribute("name", grid);
            root.appendChild(g);

            //add a text element to the child
            //Text text = doc.createTextNode("Not needed...yet");
            //child.appendChild(text);

            // Add Security info

            //create compset element, add an attribute, and add to root
            Element keypairname = doc.createElement("keypair");
            keypairname.setAttribute("keypair", keypairNameField.getText());
            root.appendChild(keypairname);

            Element keypairloc = doc.createElement("publickey");
            keypairloc.setAttribute("keypairLoc", keypairLocField.getText());
            root.appendChild(keypairloc);

            Element privatekeyloc = doc.createElement("privatekey");
            privatekeyloc.setAttribute("privatekeyLoc", privatekeyLocField.getText());
            root.appendChild(privatekeyloc);

            Element certificateloc = doc.createElement("certificate");
            certificateloc.setAttribute("certificateLoc", certificateLocField.getText());
            root.appendChild(certificateloc);




            /////////////////
            //Output the XML

            //set up a transformer
            TransformerFactory transfac = TransformerFactory.newInstance();
            Transformer trans = transfac.newTransformer();
            trans.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            trans.setOutputProperty(OutputKeys.INDENT, "yes");

            //create string from xml tree
            StringWriter sw = new StringWriter();
            StreamResult result = new StreamResult(sw);
            DOMSource source = new DOMSource(doc);
            trans.transform(source, result);
            String xmlString = sw.toString();

            //print xml
            System.out.println("Saving following to XML:\n" + xmlString);

            // Save to File
            //DataOutputStream dos=new DataOutputStream(new FileOutputStream(f));
            //dos.writeUTF(xmlString);
            //dos.close();


            // Need to replace with saveStringToFile(xmlString, file);
            BufferedWriter out = new BufferedWriter(new OutputStreamWriter(
                    new FileOutputStream(f)));
            out.append(xmlString);
            out.flush();
            out.close();

            System.out.println("File saved successfully!");



        } catch (Exception e) {
            System.out.println(e);
        }
    }

    private String[] readXML(File f) {
        //String compset, grid;
        String[] comp_grid = new String[2];
        try {
                DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
                DocumentBuilder db = dbf.newDocumentBuilder();
                Document doc = db.parse(f);

                doc.getDocumentElement().normalize();
                NodeList nodeLst = doc.getDocumentElement().getChildNodes();

                for (int s=0; s < nodeLst.getLength(); s++) {
                        Node fstNode = nodeLst.item(s);

                        if (fstNode.getNodeType() == Node.ELEMENT_NODE) {
                                NamedNodeMap atbs = fstNode.getAttributes();
                                //System.out.println(">"+fstNode.getNodeName()+"");
                                for (int k=0; k < atbs.getLength(); k++) {
                                        Node v = atbs.item(k);
                                        System.out.println("\t"+fstNode.getNodeName()+" "+ v.getNodeName()+ " = "+ v.getTextContent());
                                        if (fstNode.getNodeName().equals("compset"))
                                            comp_grid[0]=v.getTextContent();
                                        else if (fstNode.getNodeName().equals("grid"))
                                            comp_grid[1]=v.getTextContent();
                                }
                        }
                }
                

        }
        catch (Exception e) {
                //e.printStackTrace();
                comp_grid[0] = "null";
                comp_grid[1] = "null";
        }
        return comp_grid;
    }

    public static void setupOWL() {
        try {
        	// Instantiate an ontology manager
            OWLOntologyManager manager = OWLManager.createOWLOntologyManager();

            // LOAD FILE
        	// Create a file object that points to the local copy
            File file = new File(fileInLoc);

            // Now load the local copy
            OWLOntology ontology = manager.loadOntologyFromOntologyDocument(file);
            System.out.println("Loaded ontology: " + ontology);

            // We can always obtain the location where an ontology was loaded from
            IRI documentIRI = manager.getOntologyDocumentIRI(ontology);
            System.out.println("    from: " + documentIRI);
            // END LOAD FILE

            // REASONER CREATION
            // We need to create an instance of OWLReasoner.  An OWLReasoner provides the basic
            // query functionality that we need, for example the ability obtain the subclasses
            // of a class etc.  To do this we use a reasoner factory.
            // Instantiate the HermiT reasoner factory:
            OWLReasonerFactory reasonerFactory = new Reasoner.ReasonerFactory();

            OWLReasonerConfiguration config = new SimpleConfiguration();
            // Create a reasoner that will reason over our ontology and its imports closure.  Pass in the configuration.
            reasoner = reasonerFactory.createReasoner(ontology, config);
            // END REASONER CREATION

            // Ask the reasoner to do all the necessary work now
            reasoner.precomputeInferences();

            // We can determine if the ontology is actually consistent (in this case, it should be).
            boolean consistent = reasoner.isConsistent();
            System.out.println("Reasoner Consistent: " + consistent);
            System.out.println("\n");


            // factory used for queries
            fac = manager.getOWLDataFactory();
        }
        catch(UnsupportedOperationException exception) {
            System.out.println("Unsupported reasoner operation.");
        }
        catch (OWLOntologyCreationException e) {
            System.out.println("Could not load the ontology: " + e.getMessage());
        }
        /*catch (OWLException e) {
        	e.printStackTrace();
        }*/
    }

    static AmazonEC2      ec2;
    //static AmazonS3       s3;
    //static AmazonSimpleDB sdb;

    /**
     * The only information needed to create a client are security credentials
     * consisting of the AWS Access Key ID and Secret Access Key. All other
     * configuration, such as the service endpoints, are performed
     * automatically. Client parameters, such as proxies, can be specified in an
     * optional ClientConfiguration object when constructing a client.
     *
     * @see com.amazonaws.auth.BasicAWSCredentials
     * @see com.amazonaws.auth.PropertiesCredentials
     * @see com.amazonaws.ClientConfiguration
     */
    private static void initAmazon() throws Exception {
        AWSCredentials credentials = new PropertiesCredentials(
                C3_UI.class.getResourceAsStream("AwsCredentials.properties"));

        ec2 = new AmazonEC2Client(credentials);
        //s3  = new AmazonS3Client(credentials);
        //sdb = new AmazonSimpleDBClient(credentials);
    }

    public static void testAmazon() throws Exception
    {
        System.out.println("===========================================");
        System.out.println("Testing AWS Java SDK!");
        System.out.println("===========================================");

        initAmazon();

        /*
         * Amazon EC2
         *
         * The AWS EC2 client allows you to create, delete, and administer
         * instances programmatically.
         *
         * In this sample, we use an EC2 client to get a list of all the
         * availability zones, and all instances sorted by reservation id.
         */
        try {
            /*
            DescribeAvailabilityZonesResult availabilityZonesResult = ec2.describeAvailabilityZones();
            System.out.println("You have access to " + availabilityZonesResult.getAvailabilityZones().size() +
                    " Availability Zones.");

            DescribeInstancesResult describeInstancesRequest = ec2.describeInstances();

            List<Reservation> reservations = describeInstancesRequest.getReservations();
            Set<Instance> instances = new HashSet<Instance>();

            for (Reservation reservation : reservations) {
                instances.addAll(reservation.getInstances());
            }
            System.out.println("You have " + instances.size() + " Amazon EC2 instance(s) running.");
            */


            Set<Instance> newInstances;
            
            // Use either 1 or 2 below
            
            // 1 - CREATE NEW INSTANCE + get list of instances from that
            //newInstances = startNewAmazonInstance(ec2, "ami-52e2093b");
            //System.out.println("New instance created!");
            // 1 - END

            // 2 - GET CURRENT INSTANCES + get list of instances from that
            // Since we don't want to create a new instance all the time, lets use the one already existing
            newInstances = getCurrentInstances(ec2);
            // 2 - END

            // SEE ALL INSTANCES INFORMORMATION
            boolean wasPending = false;
            Iterator it = newInstances.iterator();
            while (it.hasNext()) { // For every instance created
                // Get element
                Instance newinstance = (Instance)it.next();
                System.out.println("------------------------------");
                System.out.println("Instance ID: "+newinstance.getInstanceId().toString());
                System.out.println("AMI ID used: "+newinstance.getImageId().toString());
                System.out.println("State: "+newinstance.getState().toString());
                int maxCheck = 2; // Only wait for instance to run 20 times (about 20*5 sec = 100 seconds)
                if (newinstance.getState().getName().equalsIgnoreCase("pending")) {
                    wasPending = true;              
                } else if (newinstance.getState().getName().equalsIgnoreCase("running")) {
                    System.out.println("Public IP Address: "+newinstance.getPublicIpAddress().toString());
                    System.out.println("Public DNS Name: "+newinstance.getPublicDnsName().toString());
                    System.out.println("Architecture: "+newinstance.getArchitecture());
                }
                System.out.println("------------------------------");
            }

            while (wasPending==true) {
                Thread.sleep(10000); // 10 sec
                newInstances = getCurrentInstances(ec2);
                wasPending = false;
                int maxCheck = 10; // Only wait for instance to run 20 times (about 20*5 sec = 100 seconds)
                it = newInstances.iterator();
                while (it.hasNext()) { // For every instance created
                    // Get element
                    Instance newinstance = (Instance)it.next();
                    if (newinstance.getState().getName().equalsIgnoreCase("pending")) {
                        wasPending = true;
                        System.out.println("Waiting for Amazon instance to start running... Will check " + maxCheck + " more times...");
                        System.out.println("Current State of Instance "+newinstance.getInstanceId().toString()+": "+newinstance.getState().toString());
                        maxCheck--;
                    }
                }
            }
            
            // SEE ALL INSTANCES INFORMORMATION
            it = newInstances.iterator();
            while (it.hasNext()) { // For every instance created
                // Get element
                Instance newinstance = (Instance)it.next();
                System.out.println("------------------------------");
                System.out.println("Instance ID: "+newinstance.getInstanceId().toString());
                System.out.println("AMI ID used: "+newinstance.getImageId().toString());
                System.out.println("State: "+newinstance.getState().toString());
                int maxCheck = 20; // Only wait for instance to run 20 times (about 20*5 sec = 100 seconds)
                if (newinstance.getState().getName().equalsIgnoreCase("pending")) {
                    wasPending = true;
                    while (!newinstance.getState().getName().equalsIgnoreCase("running") && maxCheck > 0) {
                        System.out.println("Waiting for Amazon instance to start running... Will check " + maxCheck + " more times...");
                        System.out.println("Current State of Instance "+newinstance.getInstanceId().toString()+": "+newinstance.getState().toString());
                        Thread.sleep(5000); // 2000 millisec = 2 seconds
                        maxCheck--;
                    } // Delay until instance is running                    
                } else if (newinstance.getState().getName().equalsIgnoreCase("running")) {
                    System.out.println("Public IP Address: "+newinstance.getPublicIpAddress().toString());
                    System.out.println("Public DNS Name: "+newinstance.getPublicDnsName().toString());
                    System.out.println("Architecture: "+newinstance.getArchitecture());
                }
                System.out.println("------------------------------");
            }



        } catch (AmazonServiceException ase) {
                System.out.println("Caught Exception: " + ase.getMessage());
                System.out.println("Reponse Status Code: " + ase.getStatusCode());
                System.out.println("Error Code: " + ase.getErrorCode());
                System.out.println("Request ID: " + ase.getRequestId());
        }

        /*
         * Amazon EC2
         *
         * The AWS EC2 client allows you to create, delete, and administer
         * instances programmatically.
         *
         * In this sample, we use an EC2 client to get a list of all the
         * availability zones, and all instances sorted by reservation id.
         */
        

    }

    /**
     * Takes in a Compset Type, Date & Extras string value from selected values,
     * and creates a new partial compset in the ontology.
     * The class is added as a subclass of NamedCompSets
     * It then adds the 3 axioms defined by type/date/extras to the class
     * @param type
     * @param date
     * @param features
     * @return
     */
    public static OWLClass createOWLCompset(String type, String date, String features)
    {
        //OWLClass compset = new OWLClass();


        return null;
    }

    /**
     *
     * @return checks OWL ontology for compset based on current partial config
     */
    public static String[] getOWLCompset(String name) {
        // Get All CompSets as list
        String[] csets = new String[50];
        OWLClass compsetClass = fac.getOWLClass(IRI.create(prefix+name)); // name = "NamedCompSets" for example
        NodeSet<OWLClass> allCompsets = reasoner.getSubClasses(compsetClass, true);
        Set<OWLClass> clses = allCompsets.getFlattened();
        System.out.println("Subset "+name+" of Compsets: ");
        int i=0;
        for(OWLClass cls : clses) {
            csets[i++] = pm.getShortForm(cls).substring(1);
            System.out.println("    " + pm.getShortForm(cls).substring(1) );
        }
        System.out.println("\n");
        return csets;
    }
    /*
    private static void printNode( org.semanticweb.owlapi.reasoner.Node<OWLClass> node) {
        // Print out a node as a list of class names in curly brackets
        System.out.print("{");
        for(Iterator<OWLClass> it = node.getEntities().iterator(); it.hasNext(); ) {
            OWLClass cls = it.next();
            // User a prefix manager to provide a slightly nicer shorter name
            System.out.print(pm.getShortForm(cls));
            if (it.hasNext()) {
                System.out.print(" ");
            }
        }
        System.out.println("}");
    }
    */
    
    /**
    * @param args the command line arguments
    */
    public static void main(String args[]) {
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new C3_UI().setVisible(true);
            }
        });
    }

    //Create a file chooser for use by all buttons
    final JFileChooser fc = new JFileChooser();
    /* IRI prefix for ontology used */
    public static final String prefix = "http://www.c3.com/ontologies/c3InputSpecs#";
    public static final String fileInLoc = "CESM_ontology.owl";
    public static final String fileOutLoc = "CESM_ontology_modified.owl";
    public static OWLReasoner reasoner;
    public static OWLDataFactory fac;
    private static DefaultPrefixManager pm = new DefaultPrefixManager(prefix);
    private static String lastCompsetType="";

    // Template variables
    private String configTemplate; // Template that gets modified
    private String baseTemplate; // Doesn't change
    private static String templateLoc = "src/template.txt";


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton ExitButton;
    private javax.swing.JTextField casenameField;
    private javax.swing.JButton certificateBrowseButton;
    private javax.swing.JTextField certificateLocField;
    private javax.swing.JTextField compsetField;
    private javax.swing.JList compsetList;
    private javax.swing.JList compsetTypeList;
    private javax.swing.JTextArea configurationScriptTextArea;
    private javax.swing.JTextField gridField;
    private javax.swing.JList gridList;
    private javax.swing.JButton jButton10;
    private javax.swing.JButton jButton13;
    private javax.swing.JButton jButton2;
    private javax.swing.JComboBox jComboBox1;
    private javax.swing.JComboBox jComboBox2;
    private javax.swing.JComboBox jComboBox3;
    private javax.swing.JComboBox jComboBox4;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel12;
    private javax.swing.JLabel jLabel13;
    private javax.swing.JLabel jLabel14;
    private javax.swing.JLabel jLabel15;
    private javax.swing.JLabel jLabel16;
    private javax.swing.JLabel jLabel17;
    private javax.swing.JLabel jLabel18;
    private javax.swing.JLabel jLabel19;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel20;
    private javax.swing.JLabel jLabel21;
    private javax.swing.JLabel jLabel22;
    private javax.swing.JLabel jLabel23;
    private javax.swing.JLabel jLabel24;
    private javax.swing.JLabel jLabel25;
    private javax.swing.JLabel jLabel26;
    private javax.swing.JLabel jLabel27;
    private javax.swing.JLabel jLabel28;
    private javax.swing.JLabel jLabel29;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel30;
    private javax.swing.JLabel jLabel31;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JList jList10;
    private javax.swing.JList jList11;
    private javax.swing.JList jList12;
    private javax.swing.JList jList13;
    private javax.swing.JList jList14;
    private javax.swing.JList jList15;
    private javax.swing.JList jList16;
    private javax.swing.JList jList17;
    private javax.swing.JList jList2;
    private javax.swing.JList jList4;
    private javax.swing.JList jList8;
    private javax.swing.JList jList9;
    private javax.swing.JPanel jPanel11;
    private javax.swing.JPanel jPanel12;
    private javax.swing.JPanel jPanel13;
    private javax.swing.JPanel jPanel14;
    private javax.swing.JPanel jPanel15;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JPanel jPanel6;
    private javax.swing.JPanel jPanel7;
    private javax.swing.JPanel jPanel8;
    private javax.swing.JPanel jPanel9;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane10;
    private javax.swing.JScrollPane jScrollPane11;
    private javax.swing.JScrollPane jScrollPane12;
    private javax.swing.JScrollPane jScrollPane13;
    private javax.swing.JScrollPane jScrollPane14;
    private javax.swing.JScrollPane jScrollPane15;
    private javax.swing.JScrollPane jScrollPane16;
    private javax.swing.JScrollPane jScrollPane17;
    private javax.swing.JScrollPane jScrollPane18;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JScrollPane jScrollPane4;
    private javax.swing.JScrollPane jScrollPane5;
    private javax.swing.JScrollPane jScrollPane8;
    private javax.swing.JScrollPane jScrollPane9;
    private javax.swing.JTabbedPane jTabbedPane1;
    private javax.swing.JButton keypairBrowseButton;
    private javax.swing.JTextField keypairLocField;
    private javax.swing.JTextField keypairNameField;
    private javax.swing.JButton loadConfigXML;
    private javax.swing.JButton privatekeyBrowseButton;
    private javax.swing.JTextField privatekeyLocField;
    private javax.swing.JButton saveConfigButton;
    private javax.swing.JButton saveConfigToXMLButton;
    private javax.swing.JButton startInstanceButton;
    private javax.swing.JButton updateConfigScript;
    // End of variables declaration//GEN-END:variables

}
