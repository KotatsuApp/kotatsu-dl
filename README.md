# kotatsu-dl
Easy-to-use cross-platform manga downloader with a lot of manga sources supported

[![Sources count](https://img.shields.io/badge/dynamic/yaml?url=https%3A%2F%2Fraw.githubusercontent.com%2FKotatsuApp%2Fkotatsu-parsers%2Frefs%2Fheads%2Fmaster%2F.github%2Fsummary.yaml&query=total&label=manga%20sources&color=%23E9321C)](https://github.com/KotatsuApp/kotatsu-parsers)  [![AUR version](https://img.shields.io/aur/version/kotatsu-dl-git?color=%233584E4)](https://aur.archlinux.org/packages/kotatsu-dl-git)

![scr](https://github.com/user-attachments/assets/1f1d28f7-9bc1-4d55-8491-43e21242755f)

# Installation

### For Windows/Linux/Mac users
Just download the [latest release](https://github.com/KotatsuApp/kotatsu-dl/releases/latest) and use it

```shell
java -jar ./kotatsu-dl.jar
```
Java 17 or later is required

### For ArchLinux users
The package is available in AUR

```shell
yay -S kotatsu-dl-git
```
When installed from AUR the `kotatsu-dl` command will be available system-wide.

# Usage

```shell
Usage: kotatsu-dl [<options>] <link>

Options:
  --dest, --destination=<value>  Output file or directory path. Default is current directory
  --format=(cbz|zip|dir)         Output format
  -j, --jobs=<int>               Number of parallel jobs for downloading
  --throttle                     Slow down downloading to avoid blocking your IP address by server
  --chapters=<numbers or range>  Numbers of chapters to download. Can be a single numbers or range, e.g. "1-4,8,11" or "all"
  -v, --verbose                  Show more information
  -h, --help                     Show this message and exit

Arguments:
  <link>  Direct link to the manga copied from browser as is
```
