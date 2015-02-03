FROM node:0.11.13

MAINTAINER Jan Sorensen <jas@praqma.net>

RUN git clone https://github.com/CleverCloud/mongo-nodejs-todo-example.git /app
WORKDIR /app
RUN npm install express && npm install mongoose && npm install connect && npm install ejs

ADD db.js /app/

EXPOSE 8080

ENTRYPOINT node app.js

