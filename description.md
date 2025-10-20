# 在 Minecraft 中体验更逼真的载具战斗与对抗
##  - 雷达、导弹、诱饵与电子战全面进化



# Experience More Realistic Vehicle Battles in Minecraft
##  - An Evolution of Radar, Missiles, Decoys & Electronic Warfare



**MCHeli-Reforged（MCH-R）** 是经典 **MC Helicopter (MCHeli)** 模组的现代化重制/强化分支，聚焦“更真实的战斗体验”。在保留原有飞机、直升机、坦克、武器与内容包生态的基础上，引入大量新机制，并对原有代码进行重构与优化。当前仓库采用 **GPL-3.0** 开源许可。


**MCHeli-Reforged (MCH-R)** is a modernized/enhanced branch of the classic **MC Helicopter (MCHeli)** mod focusing on a “more realistic combat experience”. It keeps the original ecosystem (aircraft, helicopters, tanks, weapons, and content packs), introduces many new mechanics, and refactors/optimizes core code. The repository currently uses the **GPL-3.0** license.

---

## 社区 / Community

Discord：<https://discord.gg/aBRDAM6B3N>

QQ 群：**710540248**

BFMC 服务器群：**750557809**

Gitee：<https://gitee.com/TV90/MCH-Reforged>

GitHub：<https://github.com/TV90/MCH-Reforged>

---

## 快速说明 / Quick Note

> 你下载的 `MCH-Reforged (need unzip).jar` **不是** MCHeli-Reforged 本体，而是**包含 MCH-R 模组文件结构及其依赖**的压缩包。
> The file you downloaded, **`MCH-Reforged (need unzip).jar`**, is **not** the mod itself but a container that **includes the file structure of MCH-R and its dependencies**.

**请务必先解压该 JAR**，不要直接把它放进 `mods` 文件夹。
**You must extract this JAR** instead of placing it directly into the `mods` folder.

---

## 安装步骤 / Installation Steps

1. **解压** `MCH-Reforged (need unzip).jar`。
   **Extract** `MCH-Reforged (need unzip).jar`.

2. 将以下内容放入对应目录 / Put the following into the correct directories:

  - `.minecraft/config/`：`mcheli.cfg`
  - `.minecraft/mods/`：
    - `!+unimixins-all-1.7.10-0.1.23.jar`（依赖 / dependency）
    - `mchr-mixin.jar`（依赖 / dependency）
    - `Flan's Mod Ultimate-1.58-Reforged.jar`（可选联动 / optional integration）
    - `mcheli/`（包含 `assets/` 与 `mcheli/` 两个子目录）
  - `.minecraft/resourcepacks/`：
    - `MCHR Resourcepack/`
    - `MCHR Resourcepack (Without Cross)/`
    - 二选一启用其一即可（详见下文）/ enable **one of them** (details below)

3. （推荐）加载我们提供的 `mcheli.cfg`，以便在 PVP 场景下开启**无限弹药**与**无限燃料**。
   (Recommended) Use the provided `mcheli.cfg` to enable **unlimited ammo** and **unlimited fuel** for PVP-focused gameplay.

---

## 解压后文件结构 / File Structure After Extraction

```text
.minecraft/
├─ config/
│  └─ mcheli.cfg
├─ mods/
│  ├─ !+unimixins-all-1.7.10-0.1.23.jar
│  ├─ Flan's Mod Ultimate-1.58-Reforged.jar
│  ├─ mchr-mixin.jar
│  ├─ readme.txt
│  └─ mcheli/
│     ├─ assets/
│     └─ mcheli/
└─ resourcepacks/
   ├─ readme.txt
   ├─ MCHR Resourcepack/
   └─ MCHR Resourcepack (Without Cross)/
```

---

## 目录说明 / What Each Folder/File Is For

- **`.minecraft/mods/mcheli/mcheli/`**
  - **[中文]** MCH-R 的代码文件，请勿随意修改。
  - **[EN]** Code files of MCH-R — do **not** modify arbitrarily.

- **`.minecraft/mods/mcheli/assets/`**
  - **[中文]** MCH-R 资源文件，你可像对 MCHeli 1.0.3/1.0.4 那样加入内容包。
  - **[EN]** Resource files of MCH-R — you can add content packs similar to MCHeli 1.0.3/1.0.4.

- **`.minecraft/mods/!+unimixins-all-1.7.10-0.1.23.jar` 与 `mchr-mixin.jar`**
  - **[中文]** 为 MCH-R **必须**依赖；若缺失，可能出现 **“attempting to attack an invalid entity”** 报错。
  - **[EN]** **Required** dependencies; missing them may cause the **“attempting to attack an invalid entity”** error.

- **`.minecraft/mods/Flan's Mod Ultimate-1.58-Reforged.jar`**（可选 / optional）
  - **[中文]** 提供更佳第三人称载具视角、启用伞兵/运兵功能，并在使用 MCH-R 的 APS 拦截时显示拦截提示。
  - **[EN]** Improves third-person vehicle camera, enables paratrooper/troop transport features, and shows interception indicators with MCH-R’s APS.

- **`.minecraft/resourcepacks/MCHR Resourcepack` 与 `MCHR Resourcepack (Without Cross)`**
  - **[中文]** MCH-R **必须依赖**其一以启用**热成像**。`(Without Cross)` 版本移除了原版准星，更适配载具 HUD。**二选一加载**。
  - **[EN]** One of them is **required** for **thermal imaging**. The `(Without Cross)` variant removes the vanilla crosshair for a more immersive HUD. **Enable exactly one**.

> ⚠️ **注意 / Note**
> 材质包为必需项，但只需启用其中一个；否则热成像不可用。
> A resource pack is mandatory, but only **one** needs to be active; otherwise, thermal imaging won’t work.

---

## 配置建议 / Config Recommendation

- **`mcheli.cfg`（位于 `.minecraft/config/`）**
  - **[中文]** 我们的开发重点是 **PVP** 而非生存，建议开启**无限弹药**与**无限燃料**；已为你准备好对应配置。
  - **[EN]** Since development focuses on **PVP** (not survival), we recommend **unlimited ammo** and **unlimited fuel**; a ready-to-use config is provided.

---

## 兼容与联动 / Compatibility & Integrations

- **NTM’s HBM（非必需 / optional）**
  - **[中文]** MCH-R 与 **HBM** 有一定联动；可前往 <https://github.com/HbmMods/Hbm-s-Nuclear-Tech-GIT> 下载，将其置于 `mods` 文件夹。我们**推荐 x5336 版本**；更高或更低版本疑似存在 bug。
  - **[EN]** MCH-R has some integration with **HBM**. You may download it from <https://github.com/HbmMods/Hbm-s-Nuclear-Tech-GIT> and place it into `mods`. **x5336** is recommended; higher/lower versions may contain bugs.

---

## 故障排查 / Troubleshooting

- **直接把 `MCH-Reforged (need unzip).jar` 扔进 `mods/`？**
  - 不行。必须**先解压**并按上述结构放置文件。
  - **No.** You **must extract** the JAR and place files as shown above.

- **进入游戏报 “attempting to attack an invalid entity”？**
  - 检查 `!+unimixins-all-1.7.10-0.1.23.jar` 与 `mchr-mixin.jar` 是否正确放入 `.minecraft/mods/`。
  - Verify `!+unimixins-all-1.7.10-0.1.23.jar` and `mchr-mixin.jar` are present under `.minecraft/mods/`.

- **热成像无效？**
  - 确认在 `.minecraft/resourcepacks/` 中**只启用** `MCHR Resourcepack` 或 `MCHR Resourcepack (Without Cross)` 其中之一。
  - Ensure **exactly one** of the two MCHR resource packs is enabled.

---

## 许可 / License

- 本项目使用 **GPL-3.0**。
- This project is licensed under **GPL-3.0**.

---

### 致谢 / Credits

感谢原版 **MCHeli** 及所有贡献者与内容包作者。
Thanks to the original **MCHeli** and all contributors/content-pack authors.
