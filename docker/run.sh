#!/bin/bash

cn="stzb"

docker stop ${cn}
docker rm ${cn}
docker rmi registry.cn-beijing.aliyuncs.com/songyw/stzb:latest
docker pull registry.cn-beijing.aliyuncs.com/songyw/stzb:latest
docker run -itd --name ${cn} -v /home/docker/${cn}/config:/config -v /home/docker/${cn}/logs:/logs -v /etc/localtime:/etc/localtime -p 12345:80 \
--restart=on-failure registry.cn-beijing.aliyuncs.com/songyw/stzb:latest
