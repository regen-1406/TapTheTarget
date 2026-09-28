package com.example.tapthetarget

import android.os.Bundle
import android.os.CountDownTimer
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.util.Random

class MainActivity : AppCompatActivity() {

    // ==== data permainan ====
    private var skor = 0
    private val durasiPermainan = 30_000L      // 30 detik (dalam milidetik)
    private var sisaWaktuDetik = 30
    private var timer: CountDownTimer? = null
    private var sedangDijeda = false
    private val acak = Random()

    // ==== view ====
    private lateinit var frameGame: FrameLayout
    private lateinit var imgTarget: ImageView
    private lateinit var tvSkor: TextView
    private lateinit var tvWaktu: TextView
    private lateinit var btnPause: Button
    private lateinit var overlayPause: LinearLayout
    private lateinit var overlayGameOver: LinearLayout
    private lateinit var tvSkorJeda: TextView
    private lateinit var tvSkorAkhir: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 1) ambil referensi semua view
        frameGame = findViewById(R.id.frameGame)
        imgTarget = findViewById(R.id.imgTarget)
        tvSkor = findViewById(R.id.tvSkor)
        tvWaktu = findViewById(R.id.tvWaktu)
        btnPause = findViewById(R.id.btnPause)
        overlayPause = findViewById(R.id.overlayPause)
        overlayGameOver = findViewById(R.id.overlayGameOver)
        tvSkorJeda = findViewById(R.id.tvSkorJeda)
        tvSkorAkhir = findViewById(R.id.tvSkorAkhir)

        // 2) pasang event listener
        imgTarget.setOnClickListener { targetDitekan() }
        btnPause.setOnClickListener { jedaPermainan() }
        findViewById<Button>(R.id.btnLanjutkan).setOnClickListener { lanjutkanPermainan() }
        findViewById<Button>(R.id.btnUlangi).setOnClickListener { mulaiPermainan() }
        findViewById<Button>(R.id.btnMainLagi).setOnClickListener { mulaiPermainan() }

        if (savedInstanceState != null) {
            // pulihkan keadaan permainan setelah rotasi layar (pengembangan opsional)
            skor = savedInstanceState.getInt("SKOR", 0)
            sisaWaktuDetik = savedInstanceState.getInt("SISA_WAKTU", 30)
            tvSkor.text = getString(R.string.label_skor, skor)
            tvWaktu.text = getString(R.string.label_waktu, sisaWaktuDetik)
            overlayPause.visibility = View.GONE
            overlayGameOver.visibility = View.GONE
            imgTarget.visibility = View.VISIBLE
            imgTarget.isEnabled = true
            btnPause.isEnabled = true
            frameGame.post { pindahkanTarget() }
            jalankanTimer(sisaWaktuDetik * 1000L)
        } else {
            // 3) mulai permainan pertama
            mulaiPermainan()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt("SKOR", skor)
        outState.putInt("SISA_WAKTU", sisaWaktuDetik)
    }

    override fun onDestroy() {
        super.onDestroy()
        timer?.cancel()      // penting: hentikan timer agar tidak bocor
    }

    // ================= memulai / mengulang permainan (ketentuan 9) =================
    private fun mulaiPermainan() {
        // 1) reset data permainan
        skor = 0
        sisaWaktuDetik = 30
        sedangDijeda = false

        // 2) perbarui tampilan HUD
        tvSkor.text = getString(R.string.label_skor, skor)
        tvWaktu.text = getString(R.string.label_waktu, sisaWaktuDetik)

        // 3) sembunyikan semua overlay, tampilkan target
        overlayPause.visibility = View.GONE
        overlayGameOver.visibility = View.GONE
        overlayGameOver.alpha = 1f
        imgTarget.visibility = View.VISIBLE
        imgTarget.isEnabled = true
        imgTarget.scaleX = 1f
        imgTarget.scaleY = 1f
        btnPause.isEnabled = true

        // 4) taruh target di posisi acak pertama.
        //    Dipanggil lewat post{} agar ukuran FrameLayout sudah terhitung.
        frameGame.post { pindahkanTarget() }

        // 5) jalankan timer 30 detik
        jalankanTimer(durasiPermainan)
    }

    // ================= posisi acak target (ketentuan 4) =================
    private fun pindahkanTarget() {
        // 1) ukuran area permainan (FrameLayout root)
        val lebarArea = frameGame.width
        val tinggiArea = frameGame.height

        // 2) jika layout belum diukur, hentikan agar tidak error
        if (lebarArea == 0 || tinggiArea == 0) return

        // 3) sisakan jarak aman dari tepi layar
        val jarakAman = 24

        // 4) batas maksimal koordinat agar target tidak keluar layar
        val maxX = lebarArea - imgTarget.width - (jarakAman * 2)
        val maxY = tinggiArea - imgTarget.height - (jarakAman * 2)

        if (maxX <= 0 || maxY <= 0) return

        // 5) pilih koordinat acak
        val posisiX = jarakAman + acak.nextInt(maxX)
        val posisiY = jarakAman + acak.nextInt(maxY)

        // 6) pindahkan target: x dan y dihitung dari induknya (FrameLayout)
        imgTarget.x = posisiX.toFloat()
        imgTarget.y = posisiY.toFloat()
    }

    // ================= klik target -> skor & pindah (ketentuan 5 & 6) =================
    private fun targetDitekan() {
        // 1) abaikan klik bila permainan sedang dijeda
        if (sedangDijeda) return

        // 2) tambah skor & perbarui HUD
        skor += 10
        tvSkor.text = getString(R.string.label_skor, skor)

        // 3) umpan balik visual: target mengecil lalu membesar kembali
        imgTarget.animate()
            .scaleX(0.6f).scaleY(0.6f).setDuration(70)
            .withEndAction {
                imgTarget.animate().scaleX(1f).scaleY(1f).setDuration(70).start()
            }
            .start()

        // 4) tampilkan teks "+10" melayang tepat di atas target (nilai tambah)
        tampilkanPoinMelayang()

        // 5) target berpindah ke posisi acak berikutnya (ketentuan 6)
        pindahkanTarget()
    }

    // ================= teks "+10" melayang (pengembangan opsional) =================
    private fun tampilkanPoinMelayang() {
        // 1) buat TextView baru secara programatik
        val label = TextView(this).apply {
            text = "+10"
            setTextColor(0xFF86EFAC.toInt())
            textSize = 16f
            setTypeface(null, android.graphics.Typeface.BOLD)
        }

        // 2) tambahkan ke FrameLayout - view baru otomatis berada di lapisan teratas
        frameGame.addView(label)

        // 3) posisikan tepat di atas target
        label.x = imgTarget.x + 20f
        label.y = imgTarget.y - 30f

        // 4) naik sedikit lalu hilangkan, dan jangan lupa buang dari FrameLayout
        label.animate()
            .alpha(0f)
            .translationYBy(-60f)
            .setDuration(600)
            .withEndAction { frameGame.removeView(label) }
            .start()
    }

    // ================= timer 30 detik (ketentuan 3) =================
    private fun jalankanTimer(durasi: Long) {
        // hentikan timer lama bila masih berjalan (mis. saat mengulang permainan)
        timer?.cancel()

        timer = object : CountDownTimer(durasi, 1_000L) {

            // dipanggil setiap 1 detik
            override fun onTick(millisUntilFinished: Long) {
                sisaWaktuDetik = (millisUntilFinished / 1000).toInt()
                tvWaktu.text = getString(R.string.label_waktu, sisaWaktuDetik)
            }

            // dipanggil sekali saat waktu benar-benar habis
            override fun onFinish() {
                sisaWaktuDetik = 0
                tvWaktu.text = getString(R.string.label_waktu, 0)
                gameOver()
            }
        }.start()
    }

    // ================= tombol pause & lanjutkan (ketentuan 7) =================
    private fun jedaPermainan() {
        // jangan lakukan apa-apa bila sudah dijeda atau permainan sudah berakhir
        if (sedangDijeda || overlayGameOver.visibility == View.VISIBLE) return

        sedangDijeda = true

        // 1) hentikan timer (sisaWaktuDetik menyimpan posisi terakhir)
        timer?.cancel()

        // 2) target tidak bisa ditekan selama dijeda
        imgTarget.isEnabled = false

        // 3) tampilkan overlay jeda beserta skor sementara
        tvSkorJeda.text = skor.toString()
        overlayPause.alpha = 0f
        overlayPause.visibility = View.VISIBLE
        overlayPause.animate().alpha(1f).setDuration(200).start()
    }

    private fun lanjutkanPermainan() {
        if (!sedangDijeda) return

        sedangDijeda = false
        overlayPause.visibility = View.GONE

        // target aktif kembali
        imgTarget.isEnabled = true

        // jalankan timer dari sisa waktu terakhir (bukan dari 30 detik lagi)
        jalankanTimer(sisaWaktuDetik * 1000L)
    }

    // ================= game over & main lagi (ketentuan 8 & 9) =================
    private fun gameOver() {
        // 1) pastikan timer benar-benar berhenti
        timer?.cancel()
        sedangDijeda = false

        // 2) permainan berakhir: target tidak bisa ditekan lagi
        imgTarget.isEnabled = false
        btnPause.isEnabled = false
        overlayPause.visibility = View.GONE

        // 3) tampilkan skor akhir
        tvSkorAkhir.text = skor.toString()

        // 4) tampilkan overlay Game Over dengan animasi memudar
        overlayGameOver.alpha = 0f
        overlayGameOver.visibility = View.VISIBLE
        overlayGameOver.animate()
            .alpha(1f)
            .setDuration(350)
            .start()

        // 5) target disembunyikan agar tampilan akhir bersih
        imgTarget.visibility = View.GONE
    }
}
