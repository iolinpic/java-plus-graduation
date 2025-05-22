# JAVA-PLUS-GRADUATION

## Выделенные сервисы

--- 

- category-service
- event-service (ex main-service)
- request-service
- user-service
---
- stats-server (уже был отдельным и не затронут)

## Взаимодействие между сервисами
Заголовок - имя сервиса и список сервисов используемых внутри
### user-service
ни от кого не зависит
### event-service
- user-service
- request-service
- category-service
- stats-server
### request-service
- user-service
- event-service
### category-service
- event-service
### stats-server
ни от кого не зависит
## Внешнее АПИ
[main-api](/ewm-main-service-spec.json)

## Внутренне АПИ
[stats-server](/ewm-stats-service-spec.json)

Для организации внутреннего апи использовался следующий подход:
- префиксы роутов начинаются с ```/api/v1/{service}```, где service имя сервиса без суфикса -service
- ендпоинты описаны в [интерфейсах общего модуля](/core/common/src/main/java/ru/practicum/feign) 
- для передачи данных используются общие [dto объекты](/core/common/src/main/java/ru/practicum/dto)
- для обработки ошибок используется общий [RestAdvice и общий список исключений](/core/common/src/main/java/ru/practicum/exceptions)

## Конфигурация
Все конфигурирование выполняется через  spring-cloud-config и файлы конфигурации организованы согласно структуре maven проекта и расположены в [ресурсах конфиг сервера](/infra/config-server/src/main/resources)