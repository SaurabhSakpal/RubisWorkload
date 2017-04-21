#Rubis Workload Generator

Donwload the tar from following location and extract it: http://forge.ow2.org/project/download.php?group_id=44&file_id=2842

update the name of floowing parameters in bin/rubis.properties file:

*httpd_hostname*: ip of the rubis web server 

*httpd_port*: port where rubis web server is listening

*cjdbc_hostname*: same as httpd_hostname

*httpd_use_version*: PHP (it was PHP for me, what version of rubis server you are using )

*ejb_server*: same as httpd_hostname

*workload_number_of_clients_per_node*: based on how much load you want

*workload_transition_table*: once you extract the Rubis tar, the loadtion of this file will be under workload folder

*database_regions_file*: once you extract the Rubis tar, the loadtion of this file will be under database folder

*database_categories_file*: once you extract the Rubis tar, the loadtion of this file will be under database folder
