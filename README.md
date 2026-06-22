<!-- Update with new links and icons to wargames parts -->
<!-- [![Curse Forge](https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/cozy/available/curseforge_vector.svg)]() -->
<!--[![Modrinth](https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/cozy/available/modrinth_vector.svg)]() -->

[![Discord](https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/cozy/social/discord-plural_vector.svg)](https://discord.wargames.uk)

# MCHELI-Reforged Wargames Edition SRC

This repository is the source code behind [MCHELI-Reforged Wargames Edition](https://github.com/Wargames-Development/MCHELI-R-WDG-Edition), a further fork of the MCHELI-Reforged [Gitee link](https://gitee.com/TV90/MCH-Reforged), [Github Link](https://github.com/TV90/MCH-Reforged) maintained by [TV90](https://github.com/TV90). This all builds ontop of the original [1.7.10 MCHELI](https://www.curseforge.com/minecraft/mc-mods/mcheli-minecraft-helicopter-mod) created by EMB4 which was decoded by MOC many years ago since EMB4 left the modding scene.

Please checkout our [Credits Section](#meet-our-team--credits) for the full information.

<br>

![MCHELI-Reforged Wargames Edition Banner](https://github.com/Wargames-Development/SRC-MCHELI-R-WDG-Edition/blob/main/assets/MCHELI-R-Banner.png?raw=true)

Further changes are being made in the act of connection with our Wargames Edition mods and balancing features added for a better tied together experience on the Wargames Server. This fork will be kept as close to up to date with the MCHELI-O version however changes ontop of it will have been produced originally by the WDG.

## Features

### Core Systems Integration

- Integrated with WGCore for external territory and protection handling
  - Explosion block damage is conditionally blocked based on territory rules
  - Explosion entity damage can be filtered or denied
  - Player damage is restricted in protected zones (e.g. safezones)
  - Vehicle collision damage respects protection logic
- Full attribution support for all damage sources
  - Tracks player, projectile, and indirect sources
  - Enables accurate permission checks and filtering

---

### Radar & Missile Systems

- Custom beyond-visual-range (BVR) missile system
  - Extended targeting range beyond default MCHELI limits
  - Semi-active radar homing (SARH) improvements
  - Persistent missile tracking with server-authoritative updates
- Improved target selection logic
  - Prioritises optimal targets instead of nearest-only
- Custom lock box overlay for targeting UI

---

### Aircraft Radar Model

- Per-aircraft radar configuration:
  - Air radar range
  - Ground radar range
  - Stealth factor (affects detectability)
  - Radar emitter flag (used for anti-radiation behaviour)
- Improved radar display logic
- Reduced ghost and flickering contacts
- Cleaner contact tracking and updates

---

### GPS Missile Integration

- GPS / air-to-surface missiles can use JourneyMap waypoints
  - Reads waypoint coordinates directly
  - Enables navigation-based targeting workflows

---

### Combat & Weapon Handling

- Weapon reload system fixes:
  - Reload state persists per weapon slot
  - Cannot bypass reload by switching weapons
  - Resupply correctly triggers reload timing
- Prevents duplication and exploit-style behaviour

---

### Aircraft Ground Handling

- Improved taxi and ground movement behaviour:
  - Detects obstacles in front of aircraft
  - Reduces throttle when blocked
  - Prevents unrealistic launch behaviour from collisions
  - Improves stability during ground movement

---

### HUD, Radar & RWR Improvements

- Expanded RWR detection range
- Reduced radar clutter:
  - Mortar radar ignores players and irrelevant entities
- Additional aircraft and radar stats exposed:
  - Tooltips
  - HUD overlays
- ECM / jammer information exposed to HUD systems

---

### Camera & Vision Systems

- Improved thermal and night vision handling
- More consistent shader application
- Stable camera behaviour across vehicles

---

### Client Stability & Tracking

- Improved entity tracking persistence:
  - Handles world reloads correctly
  - Handles player join/leave events more reliably
- Reduces radar and HUD desync issues

---

### Gameplay Restrictions

- Mounted tool restrictions:
  - Wrench and similar tools cannot be used while mounted
  - Prevents unintended interactions and exploits

---

### Multiplayer & Versioning

- Rebranded as **Mcheli-R Wargames Edition**
- Enforced client/server version matching via mcmod.info
- Adjusted entity tracking ranges for improved multiplayer sync

## Documentation

### Documentation Coming Soon.

<!--
If there is some documentation then please include this and update the link! The website forum documentation page needs to be produced first...

We now have documentation, it is still early, so not everything might be there, you can check it out [here](https://docs.wargames.uk/<mod>)!
-->

<br>

---

## Support Us!

<!-- Update this once wargames hosting comes out properly to direct to purchase a server! -->

Are you enjoying our mod?
Consider supporting our development!

Instead of asking for donations the **Wargames Development Group** have produced a project called host.wargames.uk (yet to release) Please consider keeping an eye out for when we release support through server hosting!

## Need to get in touch?

<!-- If Discord server or contact lines via email change, update this section here. -->

Our primary way of communicating with the community is through our [Discord Server](https://discord.wargames.uk).
Join our great community today!

Feel free to send an email to dev@wargames.uk if you have any concerns about the development, or if you find dangerous issues or abuse, contact abuse@wargames.uk
Please note that this inbox will not reply to any queries or help about the mod itself, please use the discord server for that instead.

---

## Compiling a current Version

If you are annoyed by our slow releases (since we work on the server's schedule), and you can see we have done work,
feel free to compile it yourself, however it might not work due to incomplete fixes or updates!

<!-- This is a very basic guide to getting the repo setup, this is on purpose, but could be updated if things change or is wanted -->

<details>
<summary>View Detailed Steps:</summary>

1. Enter the source code directory
   1. Navigate to the location where you downloaded the sources. *it should be `C:/Users/%USER%/Downloads`*

   2. Enter the downloaded source tree.

   3. For Win11 Shift Right-Click, and select `Open in terminal` This will open a CMD instance in this location, *if this for some reason is a powershell instance please follow below:*
        1. Open a CMD window (search CMD)

        2. cd to the directory:

        ```cmd
            cd /path/to/project-root/dir/
        ```

<br>

2. Build the mod
    1. Type `gradlew build` and then click enter

    2. Wait for completion

<br>

3. Locate the mod file.
   1. Navigate to the location where you downloaded the sources. *it should be `C:/Users/%USER%/Downloads`*

   2. Enter the downloaded source tree.

   3. Navigate to `build/libs`.

   4. Grab the .jar file from there. *This mod might be unstable due to the state of current development*

4. Updating your mod
    1. You will notice if you try and use the jar to run the game you will have issues with all the textures and vehicles missing if not crashes. Follow the below steps:

    2. Take the jar and extract it using a unzipping tool (winrar, 7zip, windows extraction),

    3. Find your existing mcheli mod folder, and inside delete the "mcheli" folder. this is not the main mcheli folder shown in the mods folder but the one inside of it.

    4. Upload the mcheli folder you recieved from extracting the jar and put it in its place.

5. Alernatively you should instead go to our hosted main repo and download the mod there: [MCHELI-R/O Wargames Edition](https://github.com/Wargames-Development/MCHELI-R-WDG-Edition)

</details>

## Contributing

<!-- This is a very basic guide to getting the repo setup, this is on purpose, but could be updated if things change or is wanted -->

Anyone and everyone is welcome to contribute and help out with the project!
However, We hope you have some understanding of modding and therefore are giving basic instructions below

<details>
<summary>View Detailed Steps:</summary>

1. Follow the Step 1 from compiling the latest version above,

2. Setup the workspace
    1. Type `gradlew setupDecompWorkspace` and then click enter

    2. Wait for completion

3. Depending on your editor of choice follow one of the below:

* Intellij Idea:
    1. Generate idea files by running `gradlew idea` in the cmd.

    2. Open the .ipr file in the explorer to intellij Idea.

* Eclipse Users:
    1. Generate eclipse files by running  `gradlew eclipse` in the cmd.

    2. Select the **eclipse** folder as a workspace when opening eclipse.

</details>

### Want to join the Development Team?

We are always looking for people to assist us in our development, as our time is more pushed into the infostructure, hardware and minecraft server.
Therefore if you wish to help out in a more official way then please get in contact with us through our Discord Server. (only if you've previously worked on any other projects)

[![Discord](https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/cozy/social/discord-plural_vector.svg)](https://discord.wargames.uk)

## Meet our Team & Credits

<!-- Add Credit to the developers of any used code, models or textures, including links. -->

Another massive thank you to all the contributors and members of the development team.
We wouldn't be where we are now without the support from you all!

All Credits for the content prior is fully credited to EMB4 for creating [HMCHELI](https://www.curseforge.com/minecraft/mc-mods/mcheli-minecraft-helicopter-mod), and of course [TV90](https://gitee.com/TV90/) the main developer of MCHELI-Reforged [Gitee link](https://gitee.com/TV90/MCH-Reforged), [Github Link](https://github.com/TV90/MCH-Reforged). You can find their discord servers below:

- [MCHELI-R's Development Server](https://discord.gg/SfkbkqhkXK).

Content will be continued to be sync'd to try and keep it in line with the current MCHELI-O fork which implements changes from MCHELI-R. Changes beyond that of what can be found in the MCHELI-O is original code/adjustments including all content of the Features section having been developed by [Glac](https://github.com/RhysHopkins04) of the [WDG](https://github.com/Wargames-Development).

### Wargames Development Group Team

- [Glac](https://github.com/RhysHopkins04) - Developer
- [Barrack](https://github.com/BateNacon) - Developer
- [Ocean](https://github.com/Oceanseaj) - Advisor
- [Viking](https://github.com/snowboardman91) - Advisor

### Contributors

[![Contributors](https://contrib.rocks/image?repo=Wargames-Development/SRC-MCHELI-R-WDG-Edition)](https://github.com/Wargames-Development/SRC-MCHELI-R-WDG-Edition/graphs/contributors)

### OLD README:

To view the old README.md from the mcheli reforged developers please view [OLD README](/MCHR-README.md), and their [OLD DESCRIPTION](/description.md).
