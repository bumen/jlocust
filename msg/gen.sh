#!/usr/bin/env bash


for i in `ls ./idl/*.proto`
    do
      protoc  --proto_path=idl/ --gofast_out=../go-boomer/msg $i
      echo gen server success $i
    done


echo gen finish