Climate Cloud Configurator (C3)
---

C3 is a prototype written in java that provides a visual interface to automated [CESM](http://www.cesm.ucar.edu/models/cesm1.0/) configuration and launching in a cloud environment.

Published in IEEE Computer Society [ISSN 0840-7459](http://dx.doi.org/10.1109/MS.2011.114).

The technical paper is available online at [GeorgiaTech](http://smartech.gatech.edu/handle/1853/35010)



Usage
---

C3 has two major utilies:

1. It utilizes an OWL ontology & HermiT reasoning engine to infer configuration options.
2. It automates creation of CESM simulations on virtual machines on the Cloud, using Amazon Elastic Cloud (EC2) in this case

The visual interface is written using Java Swing as a basic proof of concept, this has been further developed into a web service called [CESM Case Maker](https://github.com/Elucidation/CESMCaseMaker).


![GUI Compset Tab](http://i.imgur.com/PKEUD.png)

The interface automates configuration by building a knowledge base of the different configuration options using an [OWL Ontology](http://en.wikipedia.org/wiki/Web_Ontology_Language), which was designed using the visual editor [Protégé](http://protege.stanford.edu/) from Stanford. 

![GUI Grids Tab](http://i.imgur.com/59CzY.png)

An inference engine ([Hermit](http://hermit-reasoner.com/))infers options from those already made, reducing the total number of selections made as well as offloading configuration knowledge requirements from the user to the reasoning engine.


![GUI Cloud Tab](http://i.imgur.com/SEbBu.png)

Finally, C3 automatically deploys a new [virtual machine instance](http://en.wikipedia.org/wiki/Amazon_Machine_Image) on the [Amazon EC2 cloud](http://aws.amazon.com/ec2/), as well as configures and runs a new [CESM simulation](http://www.cesm.ucar.edu/models/cesm1.0/).


Example subset generated from OWL ontology.

```
Getting OWL Compset subset 'B - FULLY ACTIVE components with stub glc' from ontology
Subset B of Compsets: 
    B_1850
    B_2000_CN
    B_2000
    ...
```


---
College of Computing
Georgia Institute of Technology