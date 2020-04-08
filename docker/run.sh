#!/bin/bash

dname="stzb"

docker stop ${dname}
docker rm ${dname}
docker rmi registry.cn-beijing.aliyuncs.com/songyw/stzb:latest
docker pull registry.cn-beijing.aliyuncs.com/songyw/stzb:latest
docker run -itd --name ${dname} -v /home/docker/${dname}/logs:/logs -v /etc/localtime:/etc/localtime -p 8080:80 --restart=on-failure registry.cn-beijing.aliyuncs.com/songyw/stzb:latest
