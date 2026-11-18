/**
 * NFCard-Plus 鸿蒙版三栏界面骨架。
 * NFC 读卡与协议解析待移植（参考仓库 core-protocol 模块）。
 */
@Entry
@Component
struct Index {
  @State currentTab: number = 0;
  private controller: TabsController = new TabsController();

  @Builder
  tabBuilder(title: string, index: number) {
    Column() {
      Text(title)
        .fontSize(14)
        .fontColor(this.currentTab === index ? '#1565C0' : '#666666')
    }
    .width('100%')
    .height(56)
    .justifyContent(FlexAlign.Center)
    .onClick(() => {
      this.currentTab = index;
      this.controller.changeIndex(index);
    })
  }

  build() {
    Column() {
      Text('NFCard+')
        .fontSize(22)
        .fontWeight(FontWeight.Bold)
        .width('100%')
        .padding(16)

      Tabs({ barPosition: BarPosition.End, controller: this.controller }) {
        TabContent() {
          Column({ space: 12 }) {
            Text('将公交卡贴近手机 NFC 感应区，自动识别并读取。')
            Text('（鸿蒙版 NFC 读卡能力开发中）').fontColor('#999999')
          }.padding(16).width('100%')
        }.tabBar(this.tabBuilder('自动读取', 0))

        TabContent() {
          Column({ space: 12 }) {
            Text('手动指定卡种读取（开发中）')
            Text('交通联合 / 城市互联 / 北京一卡通 / 武汉通 / 深圳通 / 标准电子钱包')
              .fontColor('#999999')
          }.padding(16).width('100%')
        }.tabBar(this.tabBuilder('手动读取', 1))

        TabContent() {
          Column({ space: 8 }) {
            Text('NFCard+').fontSize(20).fontWeight(FontWeight.Bold)
            Text('版本：1.0.0（骨架）')
            Text('项目地址：https://github.com/imgdown/NFCard-Plus')
            Text('下载地址：https://github.com/imgdown/NFCard-Plus/releases')
            Text('许可证：Apache-2.0')
          }.padding(16).width('100%').alignItems(HorizontalAlign.Start)
        }.tabBar(this.tabBuilder('关于', 2))
      }
      .layoutWeight(1)
      .onChange((index: number) => {
        this.currentTab = index;
      })
    }
    .width('100%')
    .height('100%')
  }
}
