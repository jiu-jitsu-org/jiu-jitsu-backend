FROM almir/webhook:latest

# ⬇️ root로 전환 (중요)
USER root

# docker CLI + compose 플러그인 설치 (bash는 선택; /bin/sh 스크립트면 불필요)
RUN apk add --no-cache docker-cli docker-cli-compose

# 권장: 계속 root로 두면 /var/run/docker.sock 접근 권한 문제가 줄어듭니다.
# 원하면 마지막에 원래 유저로 돌아가도 되지만, 그 경우 docker.sock 권한 이슈를 추가로 처리해야 합니다.
# USER webhook   # ← 원 이미지 유저명이 webhook이라면 이렇게 (선택)
