#!/bin/bash


docker stop stzb
docker rm stzb
docker rmi registry.cn-beijing.aliyuncs.com/songyw/stzb:latest
docker pull registry.cn-beijing.aliyuncs.com/songyw/stzb:latest
docker run -itd --name stzb -v /home/docker/stzb/config:/config -v /home/docker/stzb/logs:/logs -v /etc/localtime:/etc/localtime -p 12345:8080 --restart=on-failure registry.cn-beijing.aliyuncs.com/songyw/stzb:latest
