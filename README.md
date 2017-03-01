SMTP mail server for thesis project.

Build with Maven:
```
$ mvn package
$ java -jar target/mailserver.jar
```

Incoming mail is stored on disk in the `mail/` directory.
Other information is stored in a MySQL database, with connection details
specified in `MailDB.java`.  To set up the database, execute the scripts
located in the `sql-files/` directory.

By default, the SMTP server listens on port 25 and the web server listens on
port 8080.
