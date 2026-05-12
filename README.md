# mock-ntp

一個使用 Spring Boot 開發的 mock NTP 服務，可在 `application.yml` 中指定固定時間，並持續對所有 NTP client 回傳該時間。

## 設定

```yaml
mock:
  ntp:
    port: 123
    fixed-time: 2026-05-12T00:00:00Z
```

`fixed-time` 使用 ISO-8601 UTC 時間格式。

## 執行

```bash
./gradlew bootRun
```
