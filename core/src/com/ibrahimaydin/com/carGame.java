package com.ibrahimaydin.com;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.TimeUtils;

import java.util.Iterator;
import java.util.Random;

public class carGame extends ApplicationAdapter {
    // Oyun içi
    SpriteBatch batch;
    int gameState = 0;
    Texture durdurmaButonu;
    BitmapFont font1;
    Music explosionMusic;
    boolean patlamaOlayi = false;

    int[] yolunOrtasıX = {160, 320, 480, 650};

    // Arkaplan
    Texture background;

    // Araba
    float arabaX;
    float arabaY;
    Texture secilenCar;
    float aracBoyutX = 0;
    float aracBoyutY = 0;
    Texture mermi;
    Texture ates;
    boolean efektGoster = false;
    float efektGostermeSuresi = 0;
    float efektGostermeSuresiLimit = 0.1f;
    Array<Rectangle> mermiler;
    float mermiHizi;
    float mermiSuresi;
    float dokunusX;

    // Düşman araba
    Array<DusmanAraba> dusmanlar;
    int dusmanY;
    long lastEnemySpawnTime;
    final long enemySpawnInterval = 1000000000; // 1 saniye
    int maxEnemies = 7;
    Texture[] dusmanTextures = new Texture[6];
    Random random;

    // Çarpışma alanları
    Rectangle arabaRectangle;
    Array<Rectangle> dusmanRectangleList;
    Array<Rectangle> mermiRectangleList;

    @Override
    public void create() {
        Music backgroundMusic = Gdx.audio.newMusic(Gdx.files.internal("tank.mp3"));
        backgroundMusic.setLooping(true); // Döngü modunda çal
        backgroundMusic.play(); // Müziği çal
        explosionMusic = Gdx.audio.newMusic(Gdx.files.internal("patlama.mp3"));

        batch = new SpriteBatch();
        secilenCar = new Texture("tank.png");
        background = new Texture("background.png");
        durdurmaButonu = new Texture("durdur.png");
        aracBoyutX = (Gdx.graphics.getWidth() / 6);
        dusmanY = Gdx.graphics.getHeight();
        aracBoyutY = (Gdx.graphics.getHeight() / 6);

        font1 = new BitmapFont();
        font1.setColor(Color.WHITE);
        font1.getData().setScale(4);

        mermi = new Texture("mermi.png");
        ates = new Texture("ates.png");
        mermiler = new Array<>();
        mermiHizi = 10;  // Mermi hızını belirleyin
        mermiSuresi = 0; // İlk mermi ateşleme zamanı

        dusmanlar = new Array<>();
        dusmanTextures[0] = new Texture("blueCar.png");
        dusmanTextures[1] = new Texture("bus.png");
        dusmanTextures[2] = new Texture("greenCar.png");
        dusmanTextures[3] = new Texture("pickupCar.png");
        dusmanTextures[4] = new Texture("policeCar.png");
        dusmanTextures[5] = new Texture("purpleCar.png");

        random = new Random();

        lastEnemySpawnTime = TimeUtils.nanoTime();

        arabaX = (Gdx.graphics.getWidth() - aracBoyutX) / 2;
        arabaY = (Gdx.graphics.getHeight() - aracBoyutY) / 2;
        Gdx.input.setInputProcessor(new InputAdapter() {
            @Override
            public boolean touchDragged(int screenX, int screenY, int pointer) {
                dokunusX = screenX * Gdx.graphics.getWidth() / Gdx.graphics.getWidth();
                if (dokunusX < 200) {
                    arabaX = 160;
                } else if (dokunusX > 800) {
                    arabaX = 700;
                } else {
                    arabaX = dokunusX - aracBoyutX / 2;
                }
                return true;
            }
        });

        // Çarpışma alanlarını oluştur
        arabaRectangle = new Rectangle();
        arabaRectangle.setSize(aracBoyutX, aracBoyutY);
        dusmanRectangleList = new Array<>();
        mermiRectangleList = new Array<>();
    }

    @Override
    public void render() {
        batch.begin();
        batch.draw(background, 0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

        if (gameState == 1) {
            spawnEnemies();
            moveEnemies();
            double yeniBoyutX = aracBoyutX * 1.5;
            for (DusmanAraba enemy : dusmanlar) {
                // batch.setColor(Color.BLACK); // Siyah renk
                batch.draw(enemy.texture, enemy.x, enemy.y, (float) yeniBoyutX, aracBoyutY);
                // batch.setColor(Color.WHITE); // Renk sıfırlama
            }
        }
        if (gameState == 0) {
            if (Gdx.input.justTouched()) {
                gameState = 1;
            }
        }

        if (gameState == 2) {
            font1.draw(batch, "Game Paused! Tap To Resume!", 200, Gdx.graphics.getHeight() / 2);
            if (Gdx.input.justTouched()) {
                gameState = 1;
            }
        }
        // batch.setColor(Color.BLACK); // Siyah renk
        batch.draw(secilenCar, arabaX, 150, aracBoyutX, aracBoyutY);
        //  batch.setColor(Color.WHITE); // Renk sıfırlama
        batch.draw(durdurmaButonu, Gdx.graphics.getWidth() - durdurmaButonu.getWidth(), Gdx.graphics.getHeight() - durdurmaButonu.getHeight());

        for (Rectangle bullet : mermiler) {
            //    batch.setColor(Color.BLACK); // Siyah renk
            batch.draw(mermi, bullet.x, bullet.y);
            //  batch.setColor(Color.WHITE); // Renk sıfırlama

            if (efektGoster) {
                batch.draw(ates, arabaX, 520, aracBoyutX, aracBoyutY / 2);
            }
        }
        batch.end();

        arabaRectangle.setPosition(arabaX, 150);
        dusmanRectangleList.clear();
        for (DusmanAraba enemy : dusmanlar) {
            Rectangle dusmanRectangle = new Rectangle(enemy.x, enemy.y, aracBoyutX * 1.5f, aracBoyutY);
            dusmanRectangleList.add(dusmanRectangle);
        }
        mermiRectangleList.clear();
        for (Rectangle bullet : mermiler) {
            Rectangle mermiRectangle = new Rectangle(bullet.x, bullet.y, 10, 10);
            mermiRectangleList.add(mermiRectangle);
        }

        // Çarpışma kontrolü
        checkCollisions();

        if (efektGoster) {
            efektGostermeSuresi += Gdx.graphics.getDeltaTime();
            if (efektGostermeSuresi >= efektGostermeSuresiLimit) {
                efektGoster = false;
                efektGostermeSuresi = 0;
            }
        }

        if (Gdx.input.justTouched()) {
            float dokunusX = Gdx.input.getX();
            float dokunusY = Gdx.graphics.getHeight() - Gdx.input.getY();
            if (dokunusX >= Gdx.graphics.getWidth() - durdurmaButonu.getWidth() && dokunusY >= Gdx.graphics.getHeight() - durdurmaButonu.getHeight()) {
                if (gameState == 1) {
                    gameState = 2; // Duraklatma durumu
                } else if (gameState == 2) {
                    gameState = 1; // Devam etme durumu
                }
                return; // Butona tıklandığında diğer işlemleri yapma
            }
        }
        if (gameState == 1) {
            mermiSuresi += Gdx.graphics.getDeltaTime();
            if (mermiSuresi >= 1) {
                fireBullet();
                mermiSuresi = 0;
            }
            moveBullets();

        }
        if (patlamaOlayi) {
            // Patlama sesini çal
            explosionMusic.play();

            // Patlama sesi tamamlandığında döngüyü tekrar etme
            explosionMusic.setOnCompletionListener(new Music.OnCompletionListener() {
                @Override
                public void onCompletion(Music music) {
                    // Patlama sesi tamamlandığında yapılacak işlemler
                    // Örneğin, patlamaOlayi değişkenini sıfırlama veya patlama sesini durdurma
                    explosionMusic.stop();
                }
            });

            // Patlama olayını sıfırla veya başka bir işlem yap
            patlamaOlayi = false;
        }


    }

    private void checkCollisions() {
        // Araba ve düşman arabaların çarpışma kontrolü
        for (DusmanAraba enemy : dusmanlar) {
            Rectangle dusmanRectangle = new Rectangle(enemy.x, enemy.y, enemy.width, enemy.height);
            if (arabaRectangle.overlaps(dusmanRectangle)) {
                gameState = 0; // Oyunu bitir
                break;
            }
        }

        // Mermilerin ve düşman arabaların çarpışma kontrolü
        Iterator<Rectangle> bulletIter = mermiRectangleList.iterator();
        while (bulletIter.hasNext()) {
            Rectangle mermiRectangle = bulletIter.next();
            Iterator<DusmanAraba> enemyIter = dusmanlar.iterator();
            while (enemyIter.hasNext()) {
                DusmanAraba enemy = enemyIter.next();
                Rectangle dusmanRectangle = new Rectangle(enemy.x, enemy.y, enemy.width, enemy.height);
                if (mermiRectangle.overlaps(dusmanRectangle)) {
                    enemyIter.remove(); // Düşman arabayı yok et
                    break; // Düşman arabaların çarpışma kontrolüne devam et
                }
            }
        }
    }


    private void spawnEnemies() {
        long currentTime = TimeUtils.nanoTime();
        if (currentTime - lastEnemySpawnTime > enemySpawnInterval && dusmanlar.size < maxEnemies) {
            int randomYol = yolunOrtasıX[random.nextInt(yolunOrtasıX.length)];
            int randomTextureIndex = random.nextInt(dusmanTextures.length);
            Texture randomTexture = dusmanTextures[randomTextureIndex];
            dusmanlar.add(new DusmanAraba(randomYol, Gdx.graphics.getHeight(), aracBoyutX, aracBoyutY, randomTexture));
            lastEnemySpawnTime = currentTime;
        }
    }

    private void moveEnemies() {
        Iterator<DusmanAraba> iter = dusmanlar.iterator();
        while (iter.hasNext()) {
            DusmanAraba enemy = iter.next();
            enemy.y -= 10;

            if (enemy.y + aracBoyutY < 0) {
                iter.remove();
            }
        }
    }

    private void fireBullet() {
        if (gameState == 1) {
            Rectangle bullet = new Rectangle();
            bullet.set(dokunusX - 60, 500, 10, 10);
            mermiler.add(bullet);
            efektGoster = true;
        }
    }

    private void moveBullets() {
        Iterator<Rectangle> iter = mermiler.iterator();
        while (iter.hasNext()) {
            Rectangle bullet = iter.next();
            bullet.y += mermiHizi;

            if (bullet.y > Gdx.graphics.getHeight()) {
                iter.remove();
            }
        }
    }

    @Override
    public void dispose() {
        batch.dispose();
        background.dispose();
        mermi.dispose();
    }

    // Düşman araba sınıfı
    private class DusmanAraba {
        float x, y, width, height;
        Texture texture;

        DusmanAraba(float x, float y, float width, float height, Texture texture) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.texture = texture;
        }
    }
}