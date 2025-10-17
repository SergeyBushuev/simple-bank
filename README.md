# simple-bank
Банковское приложение на микросервисной архитектуре.

## Запуск
Запуск и развертка производится с помощью Docker Compose

`docker-compose up --build -d`

Приложение доступно по адресу `localhost:8080`

## Сервисы и подпроекты
- [bank-accounts](bank-accounts) сервис управления аккаунтами
- [bank-blocker](bank-blocker) сервис блокировки подозрительных транзакций
- [bank-cash](bank-cash) сервис пополнения и снятия денег со счета с помощью "наличных"
- [bank-configuration](bank-configuration) сервис конфигураций // удален за ненадобностью в 2.0
- [bank-exchange](bank-exchange) сервис обмена валют
- [bank-exchange-generator](bank-exchange-generator) сервис генерации курса обмена валют
- [bank-front](bank-front) фронтенд-сервис для веба
- [bank-notifications](bank-notifications) сервис нотификаций 
- [bank-transfer](bank-transfer) сервис переводов между счетами
- [gateway-server](gateway-server) Geteway-сервис для запросов 
- [commons](commons) - Библиотека стандартных функций и объектов, используемых более чем одним сервисом 

Для безопасности приложения используется авторизация на протоколе OAuth2. 
Пользователи для входа находятся в бд для сервиса `bank-accounts`. Регистрация по адресу `localhost:8080/signup` 

## Jenkins:
Создать `jenkins_kubeconfig.yaml` 

Настроить `enviroment` для `jenkins`:
- KUBECONFIG_PATH=/path/to/jenkins_kubeconfig.yaml 
- GHCR_TOKEN={github_token}
- GITHUB_USERNAME={github_username} 
- GITHUB_TOKEN={github_token}
- GITHUB_REPOSITORY={username/simple-bank} 
- DOCKER_REGISTRY={registy/username} 
- DB_PASSWORD=postgres

Запуск Jenkins `docker-compose up -d` из директории /jenkins/

Запуск приложения в Jenkins через simple-bank-helm, по адресу `hhtp://localhost:8080`

## Kafka:
### Топики: 

**topic-bank-notifications**:
- Производители: `bank-cash`, `bank-transfer`
- Потребитель: `bank-notifications`
- Порядок сообщений: не гарантируется (unordered messages)
- Обработка: потребитель возобновляет с последнего прочитанного offset

**topic-bank-exchange**:
- Производитель: `bank-exchange-generator` (генератор курсов)
- Потребитель: `bank-exchange` (обновление курсов)
- Порядок сообщений: строго упорядочен (ordered messages)
- Обработка: потребитель может пропустить устаревшие сообщения

## Logs
Для логирования и сбора метрик используется ELK-стек:
- **Elasticsearch**: Хранение 
- **Logstash**: Обработка
- **Kibana**: Визуализация

Просмотр логов в Kibana: http://localhost:30561, экран Discover