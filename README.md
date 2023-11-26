<b>Console-GIF</b>
==================================================
А simple script for displaying colored gifs to the console with ascii-characters

![gif](./example.gif)

[[original](https://media.tenor.com/8fCoAFhaseUAAAAd/aesthetic-anime.gif)]

Using
==================================================
java Main <URI/URL>
                                                
```                
options:
    --width <value>                   set image width(chars)
    --height <value>                  set image width(chars)
    --scale_x <value>                 set image x scale
    --scale_y <value>                 set image y scale
    --size <width, height>            set image width and height
    --repeat <true/false>             reply gif after end
    --ascii <256-char palette>        output ascii art with specified palette
    --static                          output static image (maybe not gif)
    --pixel                           use nearest neighbor interpolation instead of bilinear interpolation
```

<b>Note</b>: in different terminals, the smoothness, size and color of the image may vary
