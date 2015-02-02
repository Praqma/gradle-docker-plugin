DELETE FROM mysql.user ;
CREATE USER 'root'@'%' ; -- IDENTIFIED BY 'password' ;
CREATE DATABASE studentEnrollment ;
GRANT ALL ON *.* TO 'root'@'%' WITH GRANT OPTION ;
FLUSH PRIVILEGES ;

USE studentEnrollment ;

CREATE TABLE `student` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `dateOfBirth` datetime NOT NULL,
  `emailAddress` varchar(255) NOT NULL,
  `firstName` varchar(255) NOT NULL,
  `lastName` varchar(255) NOT NULL,
  `password` varchar(8) NOT NULL,
  `userName` varchar(20) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=43 DEFAULT CHARSET=latin1;


-- Test data

INSERT INTO student
VALUES (1, '2012-12-31T11:30:45', 'jas@praqma.net', 'Jan', 'Sorensen', 'password', 'user');