Climate Cloud Configurator (C3)
Originally created by Sameer Ansari for Undergraduate Research at GeorgiaTech on the topic of Reduction of Climate Modeling Configuration Complexity using Knowledge-Based Modeling & the Cloud.

C3 has two major utilies:
1. It utilizes an OWL ontology & HermiT reasoning engine to infer configuration options.
2. It automates creation of CESM simulations on virtual machines on the Cloud, using Amazon Elastic Cloud (EC2) in this case

Original C3 Technical Paper available online at http://smartech.gatech.edu/handle/1853/35010

College of Computing
Georgia Institute of Technology

--------------

May 5th 2011, the AWS interaction requires an AwsCredentials.properties file containing access key & secret access key to be able to access a user's Amazon Web Services. The keys can be found at http://aws.amazon.com/security-credentials
The 'AwsCredentials.properties' file needs to be stored in the src/ directory
Without this, the Cloud portion of the program cannot function!