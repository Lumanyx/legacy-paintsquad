package de.xenyria.splatoon.ai.entity;

import de.xenyria.splatoon.game.color.Color;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

public class AISkin {

    public static class SkinData {

        private String value,signature;
        public SkinData(String value, String signature) {

            this.value = value;
            this.signature = signature;

        }

        public String getValue() { return value; }
        public String getSignature() { return signature; }

    }

    public static SkinData randomSkin(Color color) {

        ArrayList<SkinData> data = skinData.getOrDefault(color, new ArrayList<>());
        if(data.isEmpty()) {

            return null;

        } else if(data.size() == 1) {

            return data.get(0);

        } else {

            return data.get(new Random().nextInt(data.size() - 1));

        }

    }

    private static HashMap<Color, ArrayList<SkinData>> skinData = new HashMap<>();

    static {

        addSkinData(Color.GREEN, new SkinData("eyJ0aW1lc3RhbXAiOjE1NDgyOTgyMTU5MzQsInByb2ZpbGVJZCI6IjE1MDY5OGY3OWMxNjQ5NGM4MTE5MzAyZmE0YzY3NmU5IiwicHJvZmlsZU5hbWUiOiJfVHJhc2hlciIsInNpZ25hdHVyZVJlcXVpcmVkIjp0cnVlLCJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMjY4MGY1MmM1NmRjYTYyZmI1MDA4MTgwODhkNDE5MmY0NDdkZGZlY2QwZWRhMjA0MTIyNTdiMWEyNjNkMTMyMSJ9fX0=", "cPKj+Se2MEjR1NHDvt4WkK+ZCWoatSdBix2weRFSQ5oCX2zif0kVcVbpj+aD+UG47I+RgKB182lqFkcLLmr5gKNiikt+NhuzbXTMkxiyffUvwKK3Yx8p6Rp7AFvhvSFsdkdx0GvAg3whySzDLZB54GuiGiKK6mdnW7Wwi6IcPJr2HioxTALvBpnyrxsAQO0Z6n+NUXiuLwHa4EXQCSuPcUgnrjcQmYofu4aaN2o9Bnldv1DX2+gCMM0MwkFGPAyAlUXm5PLEMHAS/2KV6f8yT0wPPW0pQP3cO7T0gw9uGAfGdZTzTe5v66BqNEMUPITjEqW9TgtE2dPG8c5hex3wIbtGkpoSve7UT6AgZsh1Qf/cMo/4IwtBCcPI3/ISPewoarf8eqZfkEL2RDYIT7hX2sR8lT/YWq6WVjPLr8dTflXKmQnX8ghhdTyv9gW1ythd2hT4cIKD7j23vOGR7qo/YRkRGuzJfpgdl2eq/tZao+rCyxN4Vrud7fco16jcqVeA1LUJZ9SWq/mPg6155Qq9t35asDNwVFNZ+nGxYYKoFbFcujcDtPCCZfogaB0FOS/v6FD4Mo2mb8KYkOF0zj6qNQIf3Jjc94XNjOGqQbI+JGiLso2wK5V3sJ12S6p39C+inSNaNR3gBzirLuSxSgV9P9KYsdWUN5dXKwoyQ4FBJfs="));
        addSkinData(Color.GREEN, new SkinData("eyJ0aW1lc3RhbXAiOjE1NDgyOTg2NzM2NzIsInByb2ZpbGVJZCI6IjE1MDY5OGY3OWMxNjQ5NGM4MTE5MzAyZmE0YzY3NmU5IiwicHJvZmlsZU5hbWUiOiJfVHJhc2hlciIsInNpZ25hdHVyZVJlcXVpcmVkIjp0cnVlLCJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMmMyYmQ5NTE2NDYzYWFmMDIwYzAxNjhjYjg0NDhlYTAzMDE5NGVjNDNhNjhlODZlNGJkYWE4MDM0YmExN2M2OCIsIm1ldGFkYXRhIjp7Im1vZGVsIjoic2xpbSJ9fX19", "Z0SsLQnkp45X9hMUOlHI9YhuY/z2d4RzPOu+Yh3zeRChrE1VSy7mvElF3JBQ2jrG9kBjQshFzRFjEEninw2KIlFXD3vMQbt9tUk98zgUGy7lX1cX2A4ukaYOHkeITvgm52whfQkqdDukN1Kdn334U0T44D69W7HlxRVv/IOkxoQVIl9qpitY65dqhywOcb0HfYVA8v8uj3Hk4Q/odHGEW0+Z6edu+oKsUFS/arGFpvawMH/SSuLhTKJw5mkWhaapgmIaGnlHVNK8UD3DxniYy8ja6ExhicY1+3VNeucyYGTqLOvihuRbKOI99V5FNfcBqQ+Vqirc/rSBTBi0AZENr6THtShLdS/pRVUi7eJqiPK+VLWJKS1ozi3K99/UOym8STMkqbIGi7AILdIGWs9L4LyjiK4Eg0l2HkeM0BWjzpdqPtXnSnIypwVjIvDK9R8RVxTwHN+IfQofytlJOWEyP1x/6Tdcii0DWaA+z4poLQPjUU25CD0o6tWOIZ4Px8opunt8oiokf+L6kcbcsl11FVvGN1b1Ldd65NLypM9Bg9/OEISs5GfU4gF8lOoz9GCrp0ns6gVSPVfUbVfuRN1BSob2ROYBN4hP0PrOfLTpW8wDebYiVxaO5/QOOpJJS5HRoggGTtT6xVf0v37EyKKtsXqLIO4TUghEsD86+XhzKVc="));
        addSkinData(Color.ORANGE, new SkinData("eyJ0aW1lc3RhbXAiOjE1NDgyOTg1MzkxMjYsInByb2ZpbGVJZCI6IjE1MDY5OGY3OWMxNjQ5NGM4MTE5MzAyZmE0YzY3NmU5IiwicHJvZmlsZU5hbWUiOiJfVHJhc2hlciIsInNpZ25hdHVyZVJlcXVpcmVkIjp0cnVlLCJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYTc3YTU0ODM5MWNlMTViNjZkODEwZmY4ZTU2MmQ3ODYxMDU0MTMyMzVjZDY5Mjg4MGE0NWRjOTRhMDBiNDA4ZCJ9fX0=", "u32G9DMHmPYm3n/84xtcoeIVAGMRAdIy9GoaURmee2/oZpyMdMiic77Y11z4eWZlbremO257EQPIKpsuR6bQ8E9Epfkqpbghi3Nea8f3Oavoe3pugUYKIckN9mVXy7d6eNs/P6Tj6I4H7oUz+JeRtY0HkTvkiVJMWL13anbMnrHmKjxcqQB8EVuEf/e4HmwtEXxYvHSsE4aLjzLoxg20HOk90CGwsIHjDuw5dTmirctGgtC9Rr5XYA6J/25mkPmaa8iaGBurWuDt7uBZEB3StgUykJXPdCU4TBBq2uE9D6khnfwRsVGavLAT2JY2Vdv+g3F3iROKzFxWcdfDR8ROzupa/TN+VAYp3cxv5EJFRBbtqVapZhkYh5yowrDVGfjeKL4eW1/F6/DOGVlAlYDw2o82SzBvdcQ/V0UIS9qFYJm0MsQk9nHwtm/h0PGSCbVY5gQc9xlJ+Woq/9rGWURQ5CZpocN3ZXSkakVyvZVsivqLgRYXaZwWiEEN5jbnSs8R5NWfbbYnryJIW3fpXDy2nFUSXBqPMtwaEImHr+dTw7KZlJPyxcOmEM+/B4HJ3/tYm5eZ2nfh+yU/xvppeKKkiNpJAcjHU9WakPyuDVpCmYGsI0WGznTCwvzN0HE0GDCf1WuLy7mK9w3Y4mQzbGbTtrHeNSYtE3k64Z8rtKK9XBE="));
        addSkinData(Color.BLUE, new SkinData("eyJ0aW1lc3RhbXAiOjE1NDgyOTg3MTc3MTAsInByb2ZpbGVJZCI6IjE1MDY5OGY3OWMxNjQ5NGM4MTE5MzAyZmE0YzY3NmU5IiwicHJvZmlsZU5hbWUiOiJfVHJhc2hlciIsInNpZ25hdHVyZVJlcXVpcmVkIjp0cnVlLCJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMmFmZGIxN2VlNWJmOTdkNmIxNzEyNTFmOGE0MmMzN2I1ZWYyZGFkMzhkNDUzOGJjYjliNWRlYzZhMjJmMzBmMSJ9fX0=", "DaMiPER4ChdeVFgELjOnIHd0/JG+itmpugFQ4toXJP+w4/4bv/9vUhuVpdjp2hpWbTBk0ExpO7Q3DfX5D/xNRouIFpdmalBooCAY5KQgKGJikJgWEkBABAhH4i1oL/7Mg4ypYr6mIzkBYHJNmYwcnuqQlSZyGQiiuAuRxuYPj4QjqQQhwfnXxjq7Xq+fmxy8myiV7nPsUuzFqSXc8e4nYQol4MMRR1GDFSWvcy86P4YFrcLktbQiKeWI4IBLqlrJU9bpNuxwwf6cjiwUI2udan2Itiil7aDo9VuaqFETQkB3U5wQ+Ow3bQBuQYG/FdsDqMOjIWTCg4VYzdUQK1K9fYb5UV8x3d6HuJ259mYfEe2F48DYbqE1prWMxpkNcxNfPo1/cA+ELZ84mMHxVqQyiP4IRsbsz3mTZQVsIclTgaU5g6c9JeOqNQc5LSbk/nVEzqIfKRDDLXVA/5bRKfp9eJfwJTBhzEU2vLXRnf+qdx3GI35p83DTdUmJJ+3ebgdU9kPRQzWgI7YJRwCI+sfzfbg9jaB9t/9BYk5plzLj5m59EDUpfkhhglCKUUrOBl+4fByFIhuiKkIjx2DjipeL6hWK9n+72EsrM1tZlYcJPw+8lE7vW6oGcImMKsi0GE9wIUI+JlHQBwVeYVtlS2lsTwxCYKTAtxuP2WBlmEy3FgU="));
        addSkinData(Color.Pink, new SkinData("eyJ0aW1lc3RhbXAiOjE1NDgyOTg4MzUzMzMsInByb2ZpbGVJZCI6IjE1MDY5OGY3OWMxNjQ5NGM4MTE5MzAyZmE0YzY3NmU5IiwicHJvZmlsZU5hbWUiOiJfVHJhc2hlciIsInNpZ25hdHVyZVJlcXVpcmVkIjp0cnVlLCJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMjUyOTRiYjZjMzc4OWRhMzY1MzRiYTEzNWZlOGU4ZDg4MzRhNTNkMWFlMTkxZTA5ZTViYjViMGM3MzdhZGUxOSIsIm1ldGFkYXRhIjp7Im1vZGVsIjoic2xpbSJ9fX19", "Djs9r/L8dg5H4FaKko8NNfLgBDmgq5/WF2V4QLnz2xn9ccAGaYDUc39cc+d8XppImAdxFLX+QhfsGTE1Yb6gv7ggOTPS7RK1XGKrr1bdj8SO2SbmEmqZPMWauxzif6XjU2tXrKLiIRoVVLewBTIUR07DDVq5P2OyEk8r6c+UTNqpgKE5zeCa+gyi1Tb86Hrq5S11bG/9htMJVlNOQvfr3God5nhKlT9lC8caoFoVggtGjSryuSBVvd8lDgG6v0eEohI3kmyg0r+xYc8VJpAsr4+Kc+wjfHQRWBM3WKhAYozThXIUWvOg5gkyDenrsdLJrLFeo+f9Mcs5YwdJkctq6TbmjoshKPUoNLUcKRWRrK3pxxFd9GSM6O8ek6QiqM4yhkldp91cJBAGldPm/+rxSaYCGJVUA6D2NmDjKP6TgdcWFEDqkTnsOs9Wt7Ucmasncn4ix6qauRbd2blbSWkK2BEKkz4sa7iYvDnzKade53SVe3BmqX0rGsdPKzAS5nacDHEaKRphhogpsprzfqAPT3pVFw2C2NjQKx72eCmcHK8aiG6sf7ovjTmBgJQ8ymWv1Za0vVG0/ywd6hxZzPdBUgNttlK23FtYawc190AS9UnWaWZ6bP0tn9BcLnvnseobGSlMPrATivU2rBmD2r/PJ2Ziy4gZLQ8gjXb+aPTfEE8="));

    }
    public static void addSkinData(Color color, SkinData data) {

        if(!skinData.containsKey(color)) {

            ArrayList<SkinData> data1 = new ArrayList<>();
            data1.add(data);
            skinData.put(color, data1);

        } else {

            skinData.get(color).add(data);

        }

    }

}
