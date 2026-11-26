# CurseForge 投稿用メタデータ（入力ガイド）

プロジェクト作成フォームに上から順に入れる想定。

## 基本情報

- **Name**: Mekanism Pipe Tweaks
- **Summary**: `summary.txt` の1文をそのまま貼る
- **Description**: `description.md` の内容をそのまま貼る（Markdown対応エディタ）
- **Logo**: `logo.png`（960x960、実機スクリーンショットのガイド部分を拡大したもの）
- **License**: GNU Lesser General Public License (LGPL) version 3 — 選択肢にあれば `LGPL-3.0`
- **Project status / maturity**: Beta 相当（実機検証済みだが公開直後）

## カテゴリ（推奨）

- Primary: **Technology**
- Additional: **Energy** / **Transport**（パイプ・輸送系のカテゴリがあれば）
- Addons 系の分類がある場合は **Mekanism** アドオンとして登録

## 環境・対応

- **Environment**: Client and Server（両方必須）
- **Game version**: Minecraft 1.21.1
- **Mod loader**: NeoForge

## リンク

- **Source**: https://github.com/code-onigiri/mekanism-pipe-tweaks
- **Issues**: https://github.com/code-onigiri/mekanism-pipe-tweaks/issues
- Wiki / Donation: なし

## 依存関係（Relations）

- **Required dependency**: Mekanism（10.7.x、1.21.1版）
- Optional / incompatible: なし

## ギャラリー画像（images/ フォルダ、この順で登録推奨）

1. `01-guide-mixed-states.png` — 混在状態のガイド表示（四隅=通常・他=遮断）
2. `02-placement-single-side.png` — 設置時はクリック面だけ接続
3. `03-all-transmitter-types.png` — 全トランスミッター種別でのガイド
4. `04-addon-transmitter-support.png` — 他アドオンのトランスミッターでも動作
5. `05-cell-select-placement.png` — セル選択設置（v0.3.0 で復活）

## ファイル投稿

### v0.3.0（最新・推奨）

- ファイル: `build/libs/mekanism_pipe_tweaks-0.3.0.jar`（GitHubリリース v0.3.0 にも添付済み）
- Display name: `Mekanism Pipe Tweaks 0.3.0 (1.21.1)`
- Release type: Release
- Game versions: 1.21.1 / NeoForge
- Changelog: `changelog-0.3.0.md`
- Required dependency: Mekanism
- クライアントとサーバーの両方で 0.3.0 が必要（ネットワークプロトコル 3）
- Minecraft 1.20.1 / Forge 版は CF には上げず、GitHub リリース `v0.3.0-1.20.1` で配布

### 旧版（残す場合）

- v0.2.0: GitHubリリース v0.2.0 の jar / Changelog: `changelog-0.2.0.md`
- v0.1.1: GitHubリリース v0.1.1 の jar / Changelog: `changelog-0.1.1.md`
- v0.1.0: GitHubリリース v0.1.0 の jar / Changelog: `changelog-0.1.0.md`

## 注意

- 「MekanismTweaks」という別Modと無関係である旨は description 末尾に明記済み
- ガイドアイコンは GregTech Modern 由来（LGPL-3.0）。クレジットとライセンスは description と jar 同梱の NOTICE に記載済み
