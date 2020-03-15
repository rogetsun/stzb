#!/bin/bash
baseimg=registry.cn-beijing.aliyuncs.com/songyw/stzb
image=${baseimg}:${1}
echo "image is ${image}"
if [[ "${1}" == "" || "${1}" == "0" ]]; then
  echo "please input tag; ${image} ?"
else
  docker build -t ${image} .
  docker images | grep ${baseimg}
  docker rmi ${baseimg}:latest
  docker tag ${image} ${baseimg}:latest
  docker push ${image}
  docker push ${baseimg}:latest
  docker images | grep ${baseimg}
fi
