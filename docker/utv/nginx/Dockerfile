FROM nginx
MAINTAINER MUSIT
RUN rm -rf /usr/share/nginx/html/*
VOLUME ["/usr/share/nginx/html"]
ENV frontend_host=localhost
ENV frontend_port=8080
RUN rm -v /etc/nginx/nginx.conf
ADD nginx.conf /etc/nginx/
RUN echo "daemon off;" >> /etc/nginx/nginx.conf
EXPOSE 80
CMD service nginx start
