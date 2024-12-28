# ScheduledAnnouncements Plugin

**ScheduledAnnouncements** is a Minecraft Paper 1.21.3 plugin that allows server administrators to schedule automated announcements using cron expressions.

## Features
- Schedule announcements using cron expressions for precise timing.
- Customise announcement prefixes and colors.
- Make announcements clickable with included URLs.
- Reload configuration in-game without restarting the server.

## Installation
1. Download the latest release [here](https://github.com/Jelly-Pudding/scheduled-Announcements/releases/latest).
2. Place the `.jar` file in your Minecraft server's `plugins` folder.
3. Restart your server to generate the default configuration file.
4. Configure the plugin (see Configuration below).
5. Use `/scheduledannouncements reload` to apply changes without restarting.

## Configuration
1. Open the `plugins/ScheduledAnnouncements/config.yml` file.
2. Customise the global settings and individual announcements as needed. There are lots of comments in the config file to help guide you.
3. Save the file.

Example `config.yml`:
```yaml
settings:
  prefix:
    text: "[Announcement]"
    color: "gold"
  message_color: "yellow"

announcements:
  server_advertise:
    cron: "0 0/30 * * * ?"
    message: "Our server is awesome!"
    prefix:
      text: "[Ad]"
      color: "green"
    message_color: "aqua"

  discord_invite:
    cron: "0 0 */3 * * ?"
    message: "Join our Discord! | https://discord.gg/yourserver"
    prefix:
      text: ""
    message_color: "light_purple"
```

## Cron Expressions
Cron expressions allow you to schedule announcements with precision. Use [this cron expression generator](https://www.freeformatter.com/cron-expression-generator-quartz.html) for help.

Format: `Second Minute Hour DayOfMonth Month DayOfWeek Year`

Examples:
- `0 0/30 * * * ?`: Every 30 minutes
- `0 0 12 * * ?`: Every day at 12:00 PM
- `0 15 10 ? * MON-FRI`: At 10:15 AM, Monday through Friday

## Colors
Use color names from [NamedTextColor](https://jd.advntr.dev/api/4.17.0/net/kyori/adventure/text/format/NamedTextColor.html) for customising your announcements.

## In-game Commands
`/scheduledannouncements reload`: Reloads the plugin configuration.

## Tips
- To add a clickable URL, append it to the message after a pipe character (|).
- Invalid cron expressions will be logged as warnings and skipped.
- You can disable the prefix for specific announcements by setting the prefix text to an empty string.

## Support Me
Donations will help me with the development of this project.

One-off donation: https://ko-fi.com/lolwhatyesme

Patreon: https://www.patreon.com/lolwhatyesme
