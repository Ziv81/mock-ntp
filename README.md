# mock-ntp

一個使用 Spring Boot 開發的 mock NTP 服務，可在 `application.yml` 中指定固定時間，或指定一段時間範圍讓伺服器持續循環回應。

## 設定

固定時間模式：

```yaml
mock:
  ntp:
    port: 123
    fixed-time: 2026-05-12T00:00:00Z
```

區間循環模式：

```yaml
mock:
  ntp:
    port: 123
    start-time: 2026-05-12T00:00:00Z
    end-time: 2026-05-12T00:05:00Z
```

`fixed-time`、`start-time`、`end-time` 都使用 ISO-8601 UTC 時間格式。`start-time` 與 `end-time` 必須一起設定，且 `end-time` 必須晚於 `start-time`。區間循環模式會從 `start-time` 開始往前走，走到 `end-time` 前就重新從 `start-time` 開始。

## 執行

```bash
./gradlew bootRun
```

啟動後，每次收到 NTP request 都會直接輸出到 stdout，包含來源 IP、來源 port、封包長度與回應時間，方便確認 Windows 是否真的有打到這台 mock server。
