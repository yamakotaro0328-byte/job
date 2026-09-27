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
- **GUI全面リニューアル** — ハブ(プロフィール・就業中の職業・クエスト・ランキング)、フィルター付き職業一覧、報酬/パークのタブ付き詳細(「あなたの報酬」を倍率込みで表示)、表彰台つきランキング、クエスト画面、プレステージ確認(ビフォー/アフター表示)、管理パネル。アイテム名はクライアント言語で自動翻訳、斜体・攻撃力表示などのノイズを除去、シフトクリック/ドラッグでのアイテム持ち出しを完全防止。文言は `messages.<言語>.gui`、職業アイコンは `job-icons` で変更可能。
- 旧バージョンの config.yml でも、新しい設定・メッセージは同梱デフォルトで自動補完されます。
