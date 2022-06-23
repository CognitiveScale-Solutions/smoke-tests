FROM ubuntu:22.04

RUN apt update && apt upgrade -y && apt install
RUN apt install software-properties-common -y
RUN apt install python3
RUN apt install python3-pip -y

#SET YOUR PIP.CONF LOCATION HERE
ADD sample.pip.conf /cortex/pip.conf

ENV PIP_CONFIG_FILE=/cortex/pip.conf
