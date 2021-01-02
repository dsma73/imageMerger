Simple Image merger.
=============
image merge or draw string on the image.

* configure
<pre>
<code>
    {
      "outputDir":"output",
      "outputFormat":"jpg",
      "mode":"text",
      "overlayDir":"overlay",
      "drawWidth":200,
      "drawHeight":200,
      "x":62,
      "y":200,
      "textSize":100,
      "textBorder":10,
      "base":"base.jpg",
      "font":"궁서체",
      "genVideo":true,
      "fontColor":"0,255,0",
      "targets":[
        "창신동","후암동","송파동","길음동","청량리","철산동","내손동"
    ]
    }
</code>
</pre>

* Usage
    java ImageMerger <config.cfg>

