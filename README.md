# EcoJobs

Paper 26.x 用ジョブプラグイン (20職業・レベル・プレステージ・パーク・ブースター・ランキング・MySQL/PAPI対応)。

## ビルド
JDK 25 + Maven: `mvn package` → `target/ecojobs-<version>.jar`

## 1.1.0 の新機能
- **デイリークエスト** `/jobs quests` — 加入中ジョブから毎日お題 (例: 鉄鉱石を壊す x40)。達成で追加報酬+経験値、全達成でボーナス。`quests.yml` に保存。管理者は `/jobs quests reset <player>`。
- **XPボスバー** — 経験値獲得時にレベル進行度を表示 (`bossbar`)。
- **報酬コマンド** — レベルアップ／特定ジョブの特定レベル／プレステージ時にコンソールコマンド実行 (`reward-commands`)。
- **放置トラップ対策** — スポナー等の湧き方別の報酬倍率 (`anti-farm.spawn-reason-multipliers`)。
- **時間あたり稼ぎ上限** — `anti-farm.max-money-per-hour` (権限 `ecojobs.bypass.hourlycap` で無視)。
- **PAPI** — `%ecojobs_quests_completed%` `%ecojobs_quests_total%` `%ecojobs_hourly_earned%`
- 旧バージョンの config.yml でも、新しい設定・メッセージは同梱デフォルトで自動補完されます。
