package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.model.PartnerFolderTransaction
import com.example.data.model.PartnerOrganization
import com.example.data.model.PartnerTransactionType
import com.example.data.model.Product
import com.example.data.model.SalesReportPeriod
import com.example.data.model.StoreProfileConfig
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PartnerDocumentExporter {

    private fun nairaFormatter(): NumberFormat =
        NumberFormat.getCurrencyInstance(Locale("en", "NG")).apply {
            maximumFractionDigits = 0
        }

    private fun drawSerializedSignature(
        canvas: android.graphics.Canvas,
        strokesData: String,
        left: Float,
        top: Float,
        width: Float,
        height: Float
    ) {
        if (strokesData.isBlank()) return
        val sigPaint = Paint().apply {
            color = Color.parseColor("#0F172A")
            strokeWidth = 2f
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            isAntiAlias = true
        }
        strokesData.split(";").forEach { stroke ->
            val pts = stroke.split(",").mapNotNull { token ->
                val xy = token.split(":")
                if (xy.size == 2) {
                    val nx = xy[0].toFloatOrNull()
                    val ny = xy[1].toFloatOrNull()
                    if (nx != null && ny != null) nx to ny else null
                } else null
            }
            if (pts.size >= 2) {
                val path = Path()
                path.moveTo(left + pts[0].first * width, top + pts[0].second * height)
                for (i in 1 until pts.size) {
                    path.lineTo(left + pts[i].first * width, top + pts[i].second * height)
                }
                canvas.drawPath(path, sigPaint)
            }
        }
    }

    fun writePartnerMouPdf(
        outputStream: OutputStream,
        partner: PartnerOrganization,
        storeProfile: StoreProfileConfig
    ) {
        val doc = PdfDocument()
        val page = doc.startPage(PdfDocument.PageInfo.Builder(595, 842, 1).create())
        val canvas = page.canvas
        val naira = nairaFormatter()

        val headerPaint = Paint().apply { color = Color.parseColor("#052E16") }
        canvas.drawRect(0f, 0f, 595f, 110f, headerPaint)

        val goldPaint = Paint().apply {
            color = Color.parseColor("#F59E0B")
            textSize = 19f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val whitePaint = Paint().apply {
            color = Color.WHITE
            textSize = 11f
            isAntiAlias = true
        }
        canvas.drawText(storeProfile.storeName.uppercase(), 36f, 42f, goldPaint)
        canvas.drawText("MEMORANDUM OF UNDERSTANDING (PRODUCT HOSTING & SALES PARTNERSHIP)", 36f, 64f, whitePaint)
        canvas.drawText("Partner ID: ${partner.id}  |  Status: ${partner.status.label}", 36f, 84f, whitePaint)

        val sectionPaint = Paint().apply {
            color = Color.parseColor("#065F46")
            textSize = 12.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val labelPaint = Paint().apply {
            color = Color.parseColor("#4B5563")
            textSize = 10.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val valuePaint = Paint().apply {
            color = Color.parseColor("#111827")
            textSize = 11f
            isAntiAlias = true
        }
        val linePaint = Paint().apply { color = Color.parseColor("#CBD5E1"); strokeWidth = 1f }

        var y = 142f
        fun drawRow(label: String, value: String) {
            canvas.drawText(label, 36f, y, labelPaint)
            canvas.drawText(value.ifBlank { "-" }.take(58), 230f, y, valuePaint)
            y += 20f
        }

        canvas.drawText("1. PARTIES TO THE AGREEMENT", 36f, y, sectionPaint); y += 18f
        drawRow("Platform Host:", "${storeProfile.storeName} (${storeProfile.phoneNumber.ifBlank { "Official Store" }})")
        drawRow("Partner Organisation:", partner.businessName)
        drawRow("Partner Owner:", partner.ownerName)
        drawRow("Partner Phone:", partner.ownerPhone)
        drawRow("Business Address:", partner.businessAddress)

        y += 6f; canvas.drawLine(36f, y, 559f, y, linePaint); y += 22f
        canvas.drawText("2. COMMERCIAL & HOSTING TERMS", 36f, y, sectionPaint); y += 18f
        drawRow("Designated Category:", partner.primaryCategory)
        drawRow("Agreement Date:", partner.agreementDate)
        drawRow("Duration of Hosting:", partner.hostingDuration)
        drawRow("Agreed Qty / Period:", "${partner.agreedQuantityPerPeriod} units (${partner.periodUnit.label})")
        drawRow("Monthly Hosting Fee:", naira.format(partner.monthlyHostingPayment))
        drawRow("Stipulated Commission:", "${String.format(Locale.US, "%.1f", partner.stipulatedCommissionPercent)}% per product sold (Editable per item)")

        y += 6f; canvas.drawLine(36f, y, 559f, y, linePaint); y += 22f
        canvas.drawText("3. FOLDER SETTLEMENT ACCOUNTS", 36f, y, sectionPaint); y += 18f
        drawRow("OPay Account:", "${partner.opayAccountNumber.ifBlank { "Pending" }} (${partner.opayAccountName.ifBlank { partner.businessName }})")
        drawRow("Moniepoint Account:", "${partner.moniepointAccountNumber.ifBlank { "Pending" }} (${partner.moniepointAccountName.ifBlank { partner.businessName }})")

        y += 8f; canvas.drawLine(36f, y, 559f, y, linePaint); y += 22f
        canvas.drawText("4. WORKFLOW & VERIFICATION CLAUSE", 36f, y, sectionPaint); y += 18f
        val clauses = listOf(
            "a) Partner uploads products & images strictly to 24/7 SHOPPING Admin Control Room for verification.",
            "b) Upon Admin verification, products batch-sync live to Supabase Cloud under ${partner.businessName}'s folder.",
            "c) Every customer payment triggers automated alerts to both ${storeProfile.storeName} and ${partner.businessName}.",
            "d) ${storeProfile.storeName} provides Daily, Weekly & Monthly sales and stock balance reports."
        )
        clauses.forEach { clause ->
            canvas.drawText(clause.take(94), 36f, y, valuePaint)
            y += 17f
        }

        y += 18f
        val boxPaint = Paint().apply {
            color = Color.parseColor("#E2E8F0")
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }
        // Left box: Partner Owner signature
        canvas.drawRect(36f, y, 280f, y + 100f, boxPaint)
        canvas.drawText("PARTNERING BUSINESS OWNER", 44f, y + 16f, labelPaint)
        drawSerializedSignature(canvas, partner.partnerSignatureStrokes, 44f, y + 22f, 224f, 52f)
        canvas.drawText("Signed: ${partner.partnerSignatureName.ifBlank { partner.ownerName }}", 44f, y + 90f, valuePaint)

        // Right box: 24/7 SHOPPING Admin signature
        canvas.drawRect(310f, y, 559f, y + 100f, boxPaint)
        canvas.drawText("FOR: ${storeProfile.storeName.uppercase()}", 318f, y + 16f, labelPaint)
        drawSerializedSignature(canvas, partner.adminCounterSignatureStrokes, 318f, y + 22f, 228f, 52f)
        val adminLabel = if (partner.adminCounterSignatureName.isNotBlank()) {
            "Signed: ${partner.adminCounterSignatureName}"
        } else {
            "Pending Admin Counter-Signature"
        }
        canvas.drawText(adminLabel, 318f, y + 90f, valuePaint)

        val footerPaint = Paint().apply { color = Color.parseColor("#6B7280"); textSize = 9f; isAntiAlias = true }
        canvas.drawText("Official Partnership Agreement Document • ${storeProfile.storeName}", 36f, 812f, footerPaint)

        doc.finishPage(page)
        doc.writeTo(outputStream)
        doc.close()
    }

    fun sharePartnerMouPdf(
        context: Context,
        partner: PartnerOrganization,
        storeProfile: StoreProfileConfig
    ) {
        try {
            val dir = File(context.cacheDir, "invoices").apply { mkdirs() }
            val safeName = partner.id.replace(Regex("[^A-Za-z0-9._-]"), "_")
            val file = File(dir, "${safeName}_MOU.pdf")
            FileOutputStream(file).use { out -> writePartnerMouPdf(out, partner, storeProfile) }
            val uri = FileProvider.getUriForFile(context, "com.aistudio.deprosboutique.kqwmxp.fileprovider", file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "${storeProfile.storeName} & ${partner.businessName} MOU (${partner.id})")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Share Signed MOU PDF"))
        } catch (_: Exception) {
            Toast.makeText(context, "Could not share MOU PDF.", Toast.LENGTH_SHORT).show()
        }
    }

    fun buildSalesAndStockReportText(
        partner: PartnerOrganization,
        period: SalesReportPeriod,
        partnerProducts: List<Product>,
        transactions: List<PartnerFolderTransaction>,
        storeProfile: StoreProfileConfig
    ): String {
        val naira = nairaFormatter()
        val nowMs = System.currentTimeMillis()
        val cutoffMs = nowMs - (period.lookbackDays * 24L * 60L * 60L * 1000L)
        val periodSales = transactions.filter {
            (it.type == PartnerTransactionType.ORDER_CONFIRMED_SALE ||
                it.type == PartnerTransactionType.CUSTOMER_ORDER_PAYMENT_ALERT) &&
                it.timestamp >= cutoffMs
        }
        val grossRevenue = periodSales.sumOf { it.grossAmount }
        val totalCommissionProfit = periodSales.sumOf { it.commissionProfitAmount }
        val totalPartnerNet = periodSales.sumOf { it.partnerNetAmount }
        val totalUnitsSold = periodSales.sumOf { it.quantity }
        val totalStockRemaining = partnerProducts.sumOf { it.stockQuantity }
        val generatedDate = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(nowMs))

        val sb = StringBuilder()
        sb.appendLine("*${storeProfile.storeName.uppercase()} — ${period.label.uppercase()}*")
        sb.appendLine("*Partner Folder:* ${partner.businessName} (${partner.id})")
        sb.appendLine("*Generated:* $generatedDate")
        sb.appendLine("*Category:* ${partner.primaryCategory}")
        sb.appendLine("")
        sb.appendLine("*FINANCIAL SUMMARY (${period.shortLabel})*")
        sb.appendLine("• Transactions / Alerts: ${periodSales.size}")
        sb.appendLine("• Total Units Sold: $totalUnitsSold")
        sb.appendLine("• Gross Sales Volume: ${naira.format(grossRevenue)}")
        sb.appendLine("• 24/7 SHOPPING Profit (${String.format(Locale.US, "%.1f", partner.stipulatedCommissionPercent)}%): ${naira.format(totalCommissionProfit)}")
        sb.appendLine("• Partner Net Payout: ${naira.format(totalPartnerNet)}")
        sb.appendLine("• Monthly Hosting Payment: ${naira.format(partner.monthlyHostingPayment)}")
        sb.appendLine("")
        sb.appendLine("*STOCK BALANCE (${partnerProducts.size} SKUs | $totalStockRemaining units total)*")
        if (partnerProducts.isEmpty()) {
            sb.appendLine("• No live partner SKUs published yet.")
        } else {
            partnerProducts.forEach { p ->
                val flag = if (p.stockQuantity <= 0) "OUT OF STOCK" else "${p.stockQuantity} left"
                sb.appendLine("• ${p.title} (${p.brand}) — $flag @ ${naira.format(p.basePrice)} (${String.format(Locale.US, "%.1f", p.partnerCommissionPercent)}% fee)")
            }
        }
        return sb.toString().trim()
    }

    fun writeSalesReportPdf(
        outputStream: OutputStream,
        partner: PartnerOrganization,
        period: SalesReportPeriod,
        partnerProducts: List<Product>,
        transactions: List<PartnerFolderTransaction>,
        storeProfile: StoreProfileConfig
    ) {
        val doc = PdfDocument()
        val page = doc.startPage(PdfDocument.PageInfo.Builder(595, 842, 1).create())
        val canvas = page.canvas
        val naira = nairaFormatter()
        val dateFmt = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())

        val cutoffMs = System.currentTimeMillis() - (period.lookbackDays * 24L * 60L * 60L * 1000L)
        val periodSales = transactions.filter {
            (it.type == PartnerTransactionType.ORDER_CONFIRMED_SALE ||
                it.type == PartnerTransactionType.CUSTOMER_ORDER_PAYMENT_ALERT) &&
                it.timestamp >= cutoffMs
        }

        val headerPaint = Paint().apply { color = Color.parseColor("#065F46") }
        canvas.drawRect(0f, 0f, 595f, 102f, headerPaint)
        val goldPaint = Paint().apply {
            color = Color.parseColor("#F59E0B")
            textSize = 18f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val whitePaint = Paint().apply { color = Color.WHITE; textSize = 11f; isAntiAlias = true }
        canvas.drawText("${storeProfile.storeName.uppercase()} • PARTNER SALES & STOCK REPORT", 32f, 38f, goldPaint)
        canvas.drawText("${period.label}  |  Folder: ${partner.businessName} (${partner.id})", 32f, 60f, whitePaint)
        canvas.drawText("Generated: ${dateFmt.format(Date())}", 32f, 80f, whitePaint)

        val secPaint = Paint().apply {
            color = Color.parseColor("#065F46")
            textSize = 12.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val bodyPaint = Paint().apply { color = Color.parseColor("#111827"); textSize = 10.5f; isAntiAlias = true }
        val subPaint = Paint().apply { color = Color.parseColor("#4B5563"); textSize = 9.5f; isAntiAlias = true }

        var y = 132f
        val gross = periodSales.sumOf { it.grossAmount }
        val profit = periodSales.sumOf { it.commissionProfitAmount }
        val net = periodSales.sumOf { it.partnerNetAmount }
        val remainingStock = partnerProducts.sumOf { it.stockQuantity }

        canvas.drawText("FINANCIAL & STOCK SUMMARY", 32f, y, secPaint); y += 20f
        canvas.drawText("Sales Orders (${period.shortLabel}): ${periodSales.size}  |  Units Sold: ${periodSales.sumOf { it.quantity }}", 32f, y, bodyPaint); y += 17f
        canvas.drawText("Gross Sales: ${naira.format(gross)}  |  24/7 Commission Profit: ${naira.format(profit)}  |  Partner Net: ${naira.format(net)}", 32f, y, bodyPaint); y += 17f
        canvas.drawText("Monthly Hosting Fee: ${naira.format(partner.monthlyHostingPayment)}  |  Live SKUs: ${partnerProducts.size}  |  Total Stock Balance: $remainingStock units", 32f, y, bodyPaint); y += 24f

        canvas.drawText("RECENT SALES TRANSACTIONS (NO CUSTOMER EMAIL)", 32f, y, secPaint); y += 18f
        if (periodSales.isEmpty()) {
            canvas.drawText("No sales recorded in this ${period.shortLabel.lowercase()} window.", 32f, y, subPaint)
            y += 18f
        } else {
            periodSales.take(12).forEach { txn ->
                if (y < 520f) {
                    val dt = dateFmt.format(Date(txn.timestamp))
                    canvas.drawText("${txn.orderId.ifBlank { txn.id }} • $dt • ${txn.customerName} (${txn.customerPhone})", 32f, y, bodyPaint)
                    y += 14f
                    canvas.drawText("Gross: ${naira.format(txn.grossAmount)} | 24/7 Profit: ${naira.format(txn.commissionProfitAmount)} | Acct: ${txn.paymentAccount.take(34)}", 42f, y, subPaint)
                    y += 17f
                }
            }
        }

        y += 10f
        canvas.drawText("PRODUCT STOCK BALANCE", 32f, y, secPaint); y += 18f
        if (partnerProducts.isEmpty()) {
            canvas.drawText("No products published in this partner folder yet.", 32f, y, subPaint)
        } else {
            partnerProducts.take(14).forEach { prod ->
                if (y < 790f) {
                    canvas.drawText("• ${prod.title.take(30)} (${prod.category}) | Stock: ${prod.stockQuantity} | Price: ${naira.format(prod.basePrice)} | Fee: ${String.format(Locale.US, "%.1f", prod.partnerCommissionPercent)}%", 32f, y, bodyPaint)
                    y += 16f
                }
            }
        }

        doc.finishPage(page)
        doc.writeTo(outputStream)
        doc.close()
    }

    fun shareSalesReportPdf(
        context: Context,
        partner: PartnerOrganization,
        period: SalesReportPeriod,
        partnerProducts: List<Product>,
        transactions: List<PartnerFolderTransaction>,
        storeProfile: StoreProfileConfig
    ) {
        try {
            val dir = File(context.cacheDir, "invoices").apply { mkdirs() }
            val safeId = partner.id.replace(Regex("[^A-Za-z0-9._-]"), "_")
            val file = File(dir, "${safeId}_${period.shortLabel}_Report.pdf")
            FileOutputStream(file).use { out ->
                writeSalesReportPdf(out, partner, period, partnerProducts, transactions, storeProfile)
            }
            val uri = FileProvider.getUriForFile(context, "com.aistudio.deprosboutique.kqwmxp.fileprovider", file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "${partner.businessName} - ${period.label} & Stock Balance")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Share Sales & Stock Report PDF"))
        } catch (_: Exception) {
            Toast.makeText(context, "Could not generate report PDF.", Toast.LENGTH_SHORT).show()
        }
    }
}
