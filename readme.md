wen-input (文輸入法) - a simple Chinese input service for Android platform.

**Features**

- type Traditional Chinese characters using Bopomofo (using `phone.cin` from libchewing)
- character suggestions when given incomplete keys, e.g.
    
    ㄊ -> 騰、堂、頭…
    
    ㄆ -> 碰、棒、培…
    
- phrase suggestions (using `tsi.src` from libchewing), e.g.
    
    這 -> 個、些、是…
    
    上 -> 頁、午、市…
    
- key component replacement, e.g.  

    ㄨㄛˇ(我) + ㄣ -> ㄨㄣˇ(穩) 

    ㄨㄣˇ(穩) + ㄗ -> ㄗㄣˇ(怎)

    ㄨㄣˇ(穩) + ˋ -> ㄨㄣˋ(問)
    
- character frequencies from Google Search result counts
    
**Wish list**
- improve loading speed
- English input
- full/half symbol keyboard
- Simplified Chinese input
- better word/phrase suggestions
- English word/phrase suggestions
- type multiple words at once
- smart suggestion(not only based on the last character, 
  but also remember previous characters)
 
