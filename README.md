# EcoJobs

Paper 26.x 用ジョブプラグイン (20職業・レベル・プレステージ・パーク・ブースター・ランキング・MySQL/PAPI対応)。

## ビルド
JDK 25 + Maven: `mvn package` → `target/ecojobs-<version>.jar`

## 開発
- `mvn package` でテスト実行+ビルド。GitHub Actions (`.github/workflows/build.yml`) が push ごとにビルドして jar を artifact に残します。
- 変更履歴は `CHANGELOG.md`。

## 1.2.0 の新機能
- **設定検証** — 起動時と `/jobs reload` で、存在しない素材名・モブ名・パーク・ポーション効果を警告(無言で報酬が出ない事故を防止)。
- **非同期保存** — 保存処理をメインスレッドから分離、YAMLはアトミック書き込み。
- **`disabled-worlds`** — 報酬・クエストを止めるワールド。
- **設置ブロック判定の永続化** — 「自分で置いたブロック」の記録をチャンクに保存。再起動や時間経過で消えず、置いた鉱石を掘り直して稼ぐ抜け道を封鎖。ピストン移動、丸石/石/玄武岩ジェネレーターにも対応。
- **AFK対策** — `anti-farm.afk-seconds`(既定180秒)視点移動・チャット等がないと報酬もクエスト進行も停止。権限 `ecojobs.bypass.afk`。
- **生涯統計** — 職業ごとの生涯獲得額・行動回数を記録(YAML/MySQL、MySQLは列を自動追加)。GUIとPAPIに表示。
- **称号** — `job-titles` でレベル帯ごとの称号(見習い→伝説)。職業別の上書きも可能。
- **レベルアップ演出** — 画面タイトル+パーティクル(マイルストーンは豪華版)。`level-up-effects`。
- **クエスト連続達成ストリーク** — 毎日全達成で全達成ボーナスが+10%ずつ増加(最大7日分)。
- **ハッピーアワー** — `scheduled-boosters` で曜日・時間指定の自動ブースター(既定はオフ)。
- **管理コマンド** — `/jobs setlevel <player> <job> <lv>` / `addxp <player> <job> <xp>` / `resetjob <player> <job|all>`
- **PAPI追加** — `%ecojobs_title_<job>%` `%ecojobs_earned_<job>%` `%ecojobs_earned_total%` `%ecojobs_actions_<job>%` `%ecojobs_quests_streak%`

## 1.1.0 の新機能
- **デイリークエスト** `/jobs quests` — 加入中ジョブから毎日お題 (例: 鉄鉱石を壊す x40)。達成で追加報酬+経験値、全達成でボーナス。`quests.yml` に保存。管理者は `/jobs quests reset <player>`。
- **XPボスバー** — 経験値獲得時にレベル進行度を表示 (`bossbar`)。
- **報酬コマンド** — レベルアップ／特定ジョブの特定レベル／プレステージ時にコンソールコマンド実行 (`reward-commands`)。
- **放置トラップ対策** — スポナー等の湧き方別の報酬倍率 (`anti-farm.spawn-reason-multipliers`)。
- **時間あたり稼ぎ上限** — `anti-farm.max-money-per-hour` (権限 `ecojobs.bypass.hourlycap` で無視)。
- **PAPI** — `%ecojobs_quests_completed%` `%ecojobs_quests_total%` `%ecojobs_hourly_earned%`
- **GUI全面リニューアル** — ハブ(プロフィール・就業中の職業・クエスト・ランキング)、フィルター付き職業一覧、報酬/パークのタブ付き詳細(「あなたの報酬」を倍率込みで表示)、表彰台つきランキング、クエスト画面、プレステージ確認(ビフォー/アフター表示)、管理パネル。アイテム名はクライアント言語で自動翻訳、斜体・攻撃力表示などのノイズを除去、シフトクリック/ドラッグでのアイテム持ち出しを完全防止。文言は `messages.<言語>.gui`、職業アイコンは `job-icons` で変更可能。
- 旧バージョンの config.yml でも、新しい設定・メッセージは同梱デフォルトで自動補完されます。
