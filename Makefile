# =====================================================================
# SMARTOMNI - Makefile tien ich cho mono-repo
# Chi can go: make <lenh>
# =====================================================================

.PHONY: help build test clean up down logs ps rebuild infra-up infra-down

help:
	@echo "SmartOmni Mono-repo - cac lenh thuong dung:"
	@echo "  make build       - Build toan bo module bang Maven (mvn clean install)"
	@echo "  make test        - Chay unit test toan bo module"
	@echo "  make infra-up    - Chi bat ha tang (Postgres, Redis, RabbitMQ) - dung khi chay service qua IDE"
	@echo "  make infra-down  - Tat ha tang"
	@echo "  make up          - Build image + chay ha tang, Nginx, 7 domain service Java, Quartz worker bang Docker"
	@echo "  make down        - Tat toan bo he thong Docker"
	@echo "  make rebuild     - Build lai tu dau (khong dung cache) + chay lai"
	@echo "  make logs        - Xem log tat ca service (Ctrl+C de thoat)"
	@echo "  make ps          - Xem trang thai cac container dang chay"
	@echo "  make clean       - Don dep target/ cua Maven"

build:
	mvn clean install

test:
	mvn test

infra-up:
	docker compose up -d postgres redis rabbitmq

infra-down:
	docker compose stop postgres redis rabbitmq

up:
	docker compose up -d --build

down:
	docker compose down

rebuild:
	docker compose build --no-cache
	docker compose up -d

logs:
	docker compose logs -f

ps:
	docker compose ps

clean:
	mvn clean
