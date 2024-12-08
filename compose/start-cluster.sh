#!/bin/bash

# Redis 클러스터 시작
docker-compose up -d

# 실행 권한 부여 및 초기화 스크립트 실행
chmod +x init-cluster.sh
./init-cluster.sh