package com.example.calculator

import android.graphics.Color
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import android.view.HapticFeedbackConstants
import android.widget.Toast
import kotlin.concurrent.thread
import kotlin.math.PI
import kotlin.math.sin

var activeMediaPlayer: MediaPlayer? = null // שומר על הנגן בחיים

// מנגינת יזכור שקטה ועצובה המיוצרת בזמן אמת (ללא צורך בקובץ חיצוני)
fun playSadMemorialMelody() {
    thread {
        try {
            val sampleRate = 22050
            val notes = listOf(440.0 to 480, 523.25 to 480, 493.88 to 480, 392.0 to 550, 329.63 to 680, 440.0 to 950)
            for ((freq, dur) in notes) {
                val numSamples = (dur * sampleRate) / 1000
                val buffer = ShortArray(numSamples)
                for (i in 0 until numSamples) {
                    val env = sin((i.toDouble() / numSamples) * PI)
                    buffer[i] = (sin(2.0 * PI * freq * i / sampleRate) * env * Short.MAX_VALUE * 0.4).toInt().toShort()
                }
                val track = AudioTrack.Builder()
                    .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
                    .setAudioFormat(AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_16BIT).setSampleRate(sampleRate).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
                    .setBufferSizeInBytes(buffer.size * 2).setTransferMode(AudioTrack.MODE_STATIC).build()
                track.write(buffer, 0, buffer.size); track.play(); Thread.sleep(dur.toLong() + 30); track.release()
            }
        } catch (_: Exception) {}
    }
}

fun MainActivity.checkEasterEggs(input: String): Boolean {
    when (input) {
        // --- 🎮 מיני-משחקים אינטראקטיביים בתוך המחשבון ---
        "1337" -> {
            MiniGameEngine.startTicTacToe(this)
            return true
        }
        "8080" -> {
            MiniGameEngine.startCalcInvaders(this)
            return true
        }
        "5555" -> {
            MiniGameEngine.startMathRacer(this)
            return true
        }
        "1010" -> {
            MiniGameEngine.startBombDefusal(this)
            return true
        }
        "7777" -> {
            MiniGameEngine.startAnimalParty(this)
            return true
        }

        // --- הביצים הקיימות (סאונד והבהובים) ---
        "123456789" -> {
            justCalculated = true
            val resId = resources.getIdentifier("cheer", "raw", packageName)
            if (resId != 0) {
                activeMediaPlayer?.release()
                activeMediaPlayer = MediaPlayer.create(this, resId)
                activeMediaPlayer?.start()
            } else {
                Toast.makeText(this, "⚠️ קובץ cheer.mp3 לא נמצא באפליקציה!", Toast.LENGTH_SHORT).show()
            }
            val orig = bgColor
            thread {
                val colors = listOf(Color.RED, Color.GREEN, Color.BLUE, Color.MAGENTA, Color.YELLOW, Color.CYAN)
                for (i in 1..15) { Handler(Looper.getMainLooper()).post { mainContent?.setBackgroundColor(colors.random()) }; Thread.sleep(100) }
                Handler(Looper.getMainLooper()).post { mainContent?.setBackgroundColor(orig) }
            }
            return true
        }
        "911" -> {
            justCalculated = true
            val resId = resources.getIdentifier("siren", "raw", packageName)
            if (resId != 0) {
                activeMediaPlayer?.release()
                activeMediaPlayer = MediaPlayer.create(this, resId)
                activeMediaPlayer?.start()
            } else {
                Toast.makeText(this, "⚠️ הסירנה חסרה! ודא שלא קוראים לקובץ siren.mp3.mp3 בגיטהאב", Toast.LENGTH_LONG).show()
            }
            val orig = bgColor
            thread {
                val colors = listOf(Color.RED, Color.BLUE)
                for (i in 1..20) { Handler(Looper.getMainLooper()).post { mainContent?.setBackgroundColor(colors[i % 2]) }; Thread.sleep(150) }
                Handler(Looper.getMainLooper()).post { mainContent?.setBackgroundColor(orig) }
            }
            return true
        }
                // 🚨 מם ה-FBI עם צליל ההלם 🚨
        "999" -> {
            primaryColor = Color.parseColor("#FF3B30")
            bgColor = Color.parseColor("#0B0E14")
            savePreferences()
            applyTheme()
            expressionView?.text = "🚨 FBI OPEN UP! 🚨"
            currentInput = "We Know What You Did 🕵️‍♂️"
            currentView?.text = currentInput
            justCalculated = true
            shakeScreen()
            
            // השמעת הסאונד (השם חייב להיות באותיות קטנות באנדרואיד)
            val resId = resources.getIdentifier("fbiopenupsfx", "raw", packageName)
            if (resId != 0) {
                activeMediaPlayer?.release()
                activeMediaPlayer = MediaPlayer.create(this, resId)
                activeMediaPlayer?.start()
            } else {
                Toast.makeText(this, "⚠️ קובץ הסאונד חסר! ודא ששינית את שמו ל-fbiopenupsfx.mp3", Toast.LENGTH_LONG).show()
            }

            thread {
                val orig = bgColor
                val cols = listOf(Color.RED, Color.BLUE)
                for (i in 1..14) { 
                    Handler(Looper.getMainLooper()).post { mainContent?.setBackgroundColor(cols[i % 2]) }
                    Thread.sleep(120) 
                }
                Handler(Looper.getMainLooper()).post { mainContent?.setBackgroundColor(orig) }
            }
            Toast.makeText(this, "🚨 סוכני ה-FBI בדרך אליך!", Toast.LENGTH_LONG).show()
            return true
        }

        // 🎗️ מחווה ל-7 באוקטובר
        "0710", "710" -> {
            primaryColor = Color.parseColor("#FFD700")
            savePreferences()
            applyTheme()
            expressionView?.text = "07/10/2023"
            currentInput = "🎗️ לא נשכח ולא נסלח 🎗️"
            currentView?.text = currentInput
            justCalculated = true
            playSadMemorialMelody()
            if (useHaptic) mainContent?.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            return true
        }

        // 💻 מצב מטריקס
        "1999" -> {
            primaryColor = Color.parseColor("#00FF00")
            bgColor = Color.parseColor("#000000")
            savePreferences()
            applyTheme()
            expressionView?.text = "Matrix"
            currentInput = "Wake up, Neo..."
            currentView?.text = currentInput
            justCalculated = true
            if (useHaptic) mainContent?.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            return true
        }

        // 🌍 רעידת האדמה בסן פרנסיסקו
        "1906" -> {
            expressionView?.text = "San Francisco"
            currentInput = "Earthquake! 🌍"
            currentView?.text = currentInput
            justCalculated = true
            shakeScreen()
            if (useHaptic) mainContent?.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            return true
        }

        // 🌌 התשובה לחיים, ליקום וכל השאר
        "42" -> {
            expressionView?.text = "The Answer to Life"
            currentInput = "42"
            currentView?.text = currentInput
            justCalculated = true
            Toast.makeText(this, "התשובה ליקום היא 42", Toast.LENGTH_SHORT).show()
            return true
        }

        // 😈 המספר של השטן
        "666" -> {
            primaryColor = Color.parseColor("#FF0000")
            savePreferences()
            applyTheme()
            expressionView?.text = "Hell"
            currentInput = "Not Today 😈"
            currentView?.text = currentInput
            justCalculated = true
            val resId = resources.getIdentifier("evil_laugh", "raw", packageName)
            if (resId != 0) {
                activeMediaPlayer?.release()
                activeMediaPlayer = MediaPlayer.create(this, resId)
                activeMediaPlayer?.start()
            } else {
                Toast.makeText(this, "⚠️ קובץ evil_laugh.mp3 חסר בתיקיית raw!", Toast.LENGTH_SHORT).show()
            }
            if (useHaptic) mainContent?.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            return true
        }

        // --- 🔥 ביצי פסחא של ממים ברשת (קצרים וארוכים) ---
        // 🐸 מם 420: Doge / Crypto Moon
        "420" -> {
            primaryColor = Color.parseColor("#00E676")
            bgColor = Color.parseColor("#0B1F13")
            savePreferences()
            applyTheme()
            expressionView?.text = "Much Calculator, Very Math 🐶"
            currentInput = "To the Moon! 🚀"
            currentView?.text = currentInput
            justCalculated = true
            Toast.makeText(this, "🐕 Wow! Such Profit!", Toast.LENGTH_SHORT).show()
            return true
        }
                // 🗿 מם 69: Nice
        "69" -> {
            expressionView?.text = "🗿 Moai Approved"
            currentInput = "Nice. 🗿"
            currentView?.text = currentInput
            justCalculated = true
            val orig = bgColor
            thread {
                val colors = listOf(Color.parseColor("#7B1FA2"), Color.parseColor("#E040FB"))
                for (i in 1..8) { Handler(Looper.getMainLooper()).post { mainContent?.setBackgroundColor(colors[i % 2]) }; Thread.sleep(110) }
                Handler(Looper.getMainLooper()).post { mainContent?.setBackgroundColor(orig) }
            }
            return true
        }

        // 💀 מם 404: Error / Task Failed Successfully
        "404" -> {
            expressionView?.text = "ERROR 404: Math Not Found"
            currentInput = "Task Failed Successfully 💀"
            currentView?.text = currentInput
            justCalculated = true
            shakeScreen()
            return true
        }

        // 📱 מם 3310: נוקיה המיתולוגי הבלתי שביר
        "3310" -> {
            primaryColor = Color.parseColor("#9CCC65")
            bgColor = Color.parseColor("#1B2613")
            savePreferences()
            applyTheme()
            expressionView?.text = "NOKIA 3310 MODE 📱"
            currentInput = "Indestructible! 🔨"
            currentView?.text = currentInput
            justCalculated = true
            shakeScreen()
            Toast.makeText(this, "המכשיר שלך כעת חסין מפני פגיעת אסטרואיד ☄️", Toast.LENGTH_LONG).show()
            return true
        }

        // 🗿 הקוד הארוך 6942069: Gigachad / Absolute Legend
        "6942069" -> {
            primaryColor = Color.parseColor("#FFD700")
            bgColor = Color.parseColor("#141105")
            savePreferences()
            applyTheme()
            expressionView?.text = "👑 GIGACHAD DETECTED 👑"
            currentInput = "🗿 Absolute Legend"
            currentView?.text = currentInput
            justCalculated = true
            shakeScreen()
            MiniGameEngine.playTttResultSound(this, true)
            Toast.makeText(this, "🗿 השגת מעמד של אגדה מוחלטת!", Toast.LENGTH_LONG).show()
            return true
        }

        // 🦝 הקוד הארוך 8008135: Calculator Spelling הקלאסי
        "8008135" -> {
            justCalculated = true
            expressionView?.text = "הפוך את המחשבון כמו בכיתה ו' 🙃"
            currentInput = "BOOBIES 😎"
            currentView?.text = currentInput
            MiniGameEngine.playSqueakSound()
            Toast.makeText(this, "החזרת אותנו לכיתה ו'! 😂", Toast.LENGTH_LONG).show()
            val orig = bgColor
            thread {
                val colors = listOf(Color.MAGENTA, Color.CYAN, Color.YELLOW)
                for (i in 1..10) { Handler(Looper.getMainLooper()).post { mainContent?.setBackgroundColor(colors[i % 3]) }; Thread.sleep(100) }
                Handler(Looper.getMainLooper()).post { mainContent?.setBackgroundColor(orig) }
            }
            return true
        }

        // 🤡 הקוד הארוך 404404404: Skill Issue
        "404404404" -> {
            justCalculated = true
            expressionView?.text = "CRITICAL SYSTEM BRUH MOMENT"
            currentInput = "Skill Issue 💀"
            currentView?.text = currentInput
            shakeScreen()
            MiniGameEngine.playTttResultSound(this, false)
            historyList.add(0, "404404404 = Skill Issue 💀")
            savePreferences()
            return true
        }

        // 🚀 הקוד הארוך 13371337: Hacker / Elite Pro NASA
        "13371337" -> {
            primaryColor = Color.parseColor("#00FF41")
            bgColor = Color.parseColor("#000000")
            savePreferences()
            applyTheme()
            justCalculated = true
            expressionView?.text = "ROOT@MAINFRAME:~# ./hack_nasa.sh"
            currentInput = "> ACCESS GRANTED 010101 💻"
            currentView?.text = currentInput
            Toast.makeText(this, "🕶️ אתה בפנים! פורץ ללוויין של נאס״א...", Toast.LENGTH_LONG).show()
            return true
        }
    }
    return false
}
