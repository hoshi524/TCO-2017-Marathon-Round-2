# TCO-2017-Marathon-Round-2

## link

https://community.topcoder.com/longcontest/?module=ViewProblemStatement&rd=16928&compid=55902
https://community.topcoder.com/longcontest/?module=ViewStandings&rd=16928

## memo

最初に与えられる情報
baseの座標
移動速度

毎ターン与えられる情報
base
    owner
    troops
troops
    owner
    size
    座標

未知な情報が多い

base growth rate 1 ~ 3
troops to

割と重要な情報で推測するルーチンが必要な気がする

codingameで同じような問題が出ていて、それの劣化感ある(UI的に)

base 20 ~ 100

growth rate は差分みれば自明

to は難しい
    troopsの識別(id無い)
        新しいtroopsは末尾にくるとかメタ情報はある
    troopsの移動パターンが割り出せるレベルしかない気がするが面倒
    成熟してきたらどうせ必要になる情報

base troops が growth に若干のボーナスがある
    troops / 100 が足される
    max base limit = 1000
    max growth = 3 なので少なくもないか

早く占領するとscoreが高い
相手のアクションなしで占領できない場合は送らない方が良い
というジレンマがある
    相手が強いことを想定すると、消極的な方が良い
        相手固定されるっぽい？
            超重用なんだが、どっかに記載あるか

RealAI と対戦するだけでは？
他のプレイヤーと戦う訳ではないっぽい気がするが

RealAI について
troops 500 ~ 1000 貯まるまで待つ
しきい値を超えたら、近いbaseに傾きをつけて troops を送る

RealAI をメタる
序盤から、 n : 1 でbaseを狙う
base を取られることをあまり気にしないで良いかも

現状のtop scoreが880k
相対スコア
    あるテストケースの何位かがスコアになる

1 turn目で growth rate が分かる
growth が高い方が当然リターンが大きいが、取得できるまでの時間が遅くてどっちが良い？

1 : 1 のゲームじゃない
    複数baseから近いbaseはtroopsを送られる可能性が高い
    端から占領した方が良い

問題的には最小費用流
未知の要素があるから基本貪欲で良い
1:1のケースは探索効果あるかも？

どういうモデルにするか

兵士を送る量が troops / 2 で指定できない
2 turn で 75% の troops を送れる
50% 未満を送るのはできないが、それ以上を送るのは事実上可能
    分けて送る場合は、データの保持が難しい
        最低でも自分が送っているtroopsを正確に管理しないと
            codingameはそこが難しかった
            何が問題点だったか覚えてない・・・

現在のbaseの状態とbaseに送られているtroopsの状態がある
    これは正確
    何が欲しいのか
        これは難しい問なのだけど、最終的にどういう状態になるのか知りたい？
            codingameの話だと当然baseは守られたので、複雑な状態になるけど RealAI はそんなことしないから単純にシミュレートして十分の可能性？

シミュレータの実装が面倒そう

opp baseが軸
    baseの評価
        growth
        位置
    n turnに取得できる
        own baseのsubsetどうするか
values
    + growth それなりに大きい
    + 中央からの距離(というか僻地かどうか) そこそこ
    + own baseが近い(相対値？)(これ不要な可能性ある？)
    - 第３勢力が近い 小さい
    - n turnで取得できる 大きい
    - 使用baseが多い 小さい
sortして上から
