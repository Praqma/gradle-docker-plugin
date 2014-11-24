#! /bin/sh
mysqld --datadir=/var/lib/mysql --user=mysql --init-file /initdb/initdb.sql &
sleep 5
mysqladmin -u root shutdown
