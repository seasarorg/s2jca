このプロジェクトをビルドおよびテストするにはActiveMQ が必要です．

1．以下からActiveMQをダウンロードし，ファイルシステム上に展開してください．
http://activemq.codehaus.org/Download

2．クラスパス変数を設定してください．
メニューで「Window」－「Preferences」を選択します．
左のペインで「Java」－「Build Path」－「Classpath Variables」を選択します．
「New」ボタンを押下します．
「Name」に"ACTIVQMQ_HOME"，「Path」にActiveMQを展開したディレクトリを指定します．
「OK」ボタンを押下します．

3．テスト開始前にActiveMQブローカーを実行します．
ActiveMQを展開したディレクトリのbin/activemqを実行します．