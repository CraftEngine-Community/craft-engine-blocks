# CraftEngine Extensions


<details>
<summary><strong>English version</strong></summary>

> Translated by AI for reference purposes only.

## Extended Block Behaviors
- [X] Adjustable Redstone Block
- [X] Pickaxe Block
- [X] Placer Block
- [X] Chair Block (Block Entity)

## Extended Item Behaviors
- [X] Place Block

## Extended Event Functions
- [X] Apply Item Data Function
- [X] Set Item Tooltip Description/Lore Function

## Extended Item Settings
-[X] Dynamic Attributes

## Extended Item Data
- [X] Fixed Random Value (Used for randomizing dynamic attribute values)
- [X] Get Parameter (Only used for Client-Bound Data to retrieve fixed random value parameters from the item)
- [X] Enchantment Lore (Suitable for customizing enchantment display effects via Client-Bound Data)

## References
I adapted some code from the following open-source projects:

- [CraftEngine](https://github.com/Xiao-MoMi/craft-engine)
- [LuckPerms](https://github.com/LuckPerms/LuckPerms)

## Example Configurations

### Pickaxe Block & Placer Block

[block_extension_demo.zip](https://www.google.com/search?q=demo/block_extension_demo.zip)

### Enchantment Lore

```yaml
items:
  default:topaz_pickaxe:
    material: golden_pickaxe
    client-bound-data:
      hide-tooltip:
        - enchantments
      gtemc:enchantments_lore:
        efficiency: |-
          <!i><gray><lang:enchantment.%id_namespace%.%id_value%> <lang:enchantment.level.%level%>
            Increases mining speed by <green><expr:0.##:'20 + 5 * %level%'>%</green>

```

</details>

## 拓展方块行为
- [X] 可调节红石块
- [X] 镐方块
- [X] 放置方块
- [X] 座椅方块 (方块实体)

## 拓展物品行为
- [X] 放置方块

## 拓展事件函数
- [X] 应用物品数据函数
- [X] 设置物品提示框中的描述信息函数

## 拓展物品设置
- [X] 动态属性

## 拓展物品数据
- [X] 固定随机值 (用于动态属性数值随机化)
- [X] 获取参数 (仅用于客户端侧物品数据从物品获取固定随机值参数)
- [X] 附魔描述 (适用于通过客户端侧物品数据自定义附魔显示的效果)

## 参考项目
我从以下开源项目借鉴了一些代码
- [CraftEngine](https://github.com/Xiao-MoMi/craft-engine)
- [LuckPerms](https://github.com/LuckPerms/LuckPerms)

## 示例配置

### 镐方块 & 放置方块

[block_extension_demo.zip](demo/block_extension_demo.zip)

### 附魔描述

```yaml
items:
  default:topaz_pickaxe:
    material: golden_pickaxe
    client-bound-data:
      hide-tooltip:
        - enchantments
      gtemc:enchantments_lore:
        efficiency: |-
          <!i><gray><lang:enchantment.%id_namespace%.%id_value%> <lang:enchantment.level.%level%>
            增加 <green><expr:0.##:'20 + 5 * %level%'>%</green> 挖掘速度
```
