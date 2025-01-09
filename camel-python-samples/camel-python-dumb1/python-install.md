How I installed python on Mac:

1. Install python.
brew install python

This installed python 3

2. check versions
$ python3 --version
$ pip3 --version

3. Install requests (to call HTTP requests)
$ python3 -m venv myenv
$ source myenv/bin/activate
$ pip3 install requests


Note: it took me a while to figure out that pip3 install requests. So for some reason I installed `pipx`, and I have runned
$ pipx ensurepath

And this has set `~/.local/bin` in the path, but my guess is  python3 install should have done this too. 


# How I Installed Python on Mac

1. Install Python
```shell
   brew install python
```

This installed Python 3.

2. Check Versions
```shell
   python3 --version
   pip3 --version
```

3. Install Requests (to call HTTP requests)
```shell
   python3 -m venv myenv
   source myenv/bin/activate
   pip3 install requests
```

# Random install

It took me a while to figure out that `pip3 install requests`. So for some  reason, I installed `pipx`, and I have runned
```shell
  pipx ensurepath
```

And this has set `~/.local/bin` in the path, but my guess is Python 3  should have done this too.


