# fontutil3

OpenJDK11 + OpenJFX11 + maven にて font 關聯の 道具を 提供する program。
開發には Apache NetBeans を 用ゐたり。
特に 漢字 fonts を target と する program にて 別の 手書き認識 program の 學習 data の template (手本) を 抽出するが 目的なり。
なほ Windows 上にて 動かすには ```pom.xml``` file の path separator を 換る 要 有り。

## Build & Execute

```
$ mvn package
$ mvn exec:exec
```

## Test1

開く font file(s) の glyph の 有無を 表す yaml file を 出力す。
必要とする 漢字群を canvas に 全て 表示するは 昔の font file にて glyph 無けれども 空白を 表示しつる ものが 有る 故。
XML ならず JSON ならず yaml を 用ゐるは 必要なる 行までを 讀みて 解析し易き 故。
