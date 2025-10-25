package com.ipev.pesocorrigido;

import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.ipev.pesocorrigido.databinding.ActivityMainBinding;

import java.util.ArrayList;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private double somaPeso, pesoAlvo, altitudeInicial, altitudeMeta, TempMeta;

    double Ro = 287.053;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // ✅ Inflate antes do setContentView
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // ✅ Oculta ActionBar e StatusBar (modo tela cheia)
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN);

        setListeners();
    }

    private void setListeners() {
        TextWatcher calculoPeso = new SimpleTextWatcher(this::calcularPeso);
        TextWatcher calculoTudo = new SimpleTextWatcher(this::calcularTudo);

        binding.textEditPesoInicial.addTextChangedListener(calculoPeso);
        binding.textEditLastro.addTextChangedListener(calculoPeso);
        binding.textEditFuel.addTextChangedListener(calculoPeso);

        binding.textEditAtt.addTextChangedListener(calculoTudo);
        binding.textEditTemp.addTextChangedListener(calculoTudo);
        binding.textEditAlvo.addTextChangedListener(calculoTudo);
    }

    // 🔹 TextWatcher simplificado via callback
    private static class SimpleTextWatcher implements TextWatcher {
        private final Runnable afterTextChangedAction;

        SimpleTextWatcher(Runnable action) {
            this.afterTextChangedAction = action;
        }

        @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
        @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
        @Override public void afterTextChanged(Editable s) { afterTextChangedAction.run(); }
    }

    private void calcularPeso() {
        String pesoStr = binding.textEditPesoInicial.getText().toString().trim();
        String lastroStr = binding.textEditLastro.getText().toString().trim();
        String fuelStr = binding.textEditFuel.getText().toString().trim();

        if (pesoStr.isEmpty() || lastroStr.isEmpty() || fuelStr.isEmpty()) return;

        try {
            double pesoInicial = Double.parseDouble(pesoStr);
            double lastro = Double.parseDouble(lastroStr);
            double fuel = Double.parseDouble(fuelStr);
            somaPeso = pesoInicial + fuel + lastro;

            binding.textEditPesoTotal.setText(String.format(Locale.US, "%.0f", somaPeso));
            calcularTudo(); // Atualiza dependências automaticamente

        } catch (NumberFormatException e) {
            Toast.makeText(this, "Erro ao converter números.", Toast.LENGTH_SHORT).show();
        }
    }

    private void calcularnovaAlt() {

    }

    private void calcularTudo() {
        String altitudeStr = binding.textEditAtt.getText().toString().trim();
        String tempStr = binding.textEditTemp.getText().toString().trim();
        String pesoAlvoStr = binding.textEditAlvo.getText().toString().trim();



        if (altitudeStr.isEmpty() || tempStr.isEmpty() || pesoAlvoStr.isEmpty()) return;

        try {


            pesoAlvo = Double.parseDouble(pesoAlvoStr);
            altitudeInicial = Double.parseDouble(altitudeStr);
            double pa = calcularPa(altitudeInicial);

            double temp = Double.parseDouble(tempStr);
            double k = calcularK(temp);

            double rho = calcularRho(pa, k, Ro);
            double rho2 = calcularRho2(somaPeso, pesoAlvo);

            double sigma = calcularSigma(rho);

            binding.textEditSigma.setText(String.format(Locale.US, "%.6f", sigma));

            double pesoCorrigido = somaPeso / sigma;

            altitudeMeta = calcularPaux(k, pa, rho2, altitudeInicial);

            binding.textEditPesoCorrigido.setText(String.format(Locale.US, "%.0f", pesoCorrigido));

            setarPesoAlvoGrafico(pesoCorrigido);

        } catch (NumberFormatException e) {
            Toast.makeText(this, "Erro ao converter números.", Toast.LENGTH_SHORT).show();
        }
    }

    private void setarPesoAlvoGrafico(double pesoCorrigido) {

        try {
            adicionarPontoAoGrafico(pesoAlvo, pesoCorrigido);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Erro ao converter peso alvo.", Toast.LENGTH_SHORT).show();
        }
    }


    private void adicionarPontoAoGrafico(double pesoAlvo_, double pesoCorrigido_) {
        ArrayList<Entry> entradasA = new ArrayList<>();
        ArrayList<Entry> entradasB = new ArrayList<>();
        ArrayList<Entry> entradaLimSup1 = new ArrayList<>();
        ArrayList<Entry> entradaLimInf1 = new ArrayList<>();
        ArrayList<Entry> entradaLimSup2 = new ArrayList<>();
        ArrayList<Entry> entradaLimInf2 = new ArrayList<>();

        float pesoAlvo = (float) pesoAlvo_;
        float pesoCorrigido = (float) pesoCorrigido_;

        entradasA.add(new Entry(-1f, pesoAlvo));
        entradasA.add(new Entry(1f, pesoAlvo));
        entradasB.add(new Entry(0f, pesoCorrigido));

        float limSup1 = pesoAlvo * 1.01f;
        float limInf1 = pesoAlvo * 0.99f;
        float limSup2 = pesoAlvo * 1.02f;
        float limInf2 = pesoAlvo * 0.98f;

        entradaLimSup1.add(new Entry(-1f, limSup1));
        entradaLimSup1.add(new Entry(1f, limSup1));
        entradaLimInf1.add(new Entry(-1f, limInf1));
        entradaLimInf1.add(new Entry(1f, limInf1));
        entradaLimSup2.add(new Entry(-1f, limSup2));
        entradaLimSup2.add(new Entry(1f, limSup2));
        entradaLimInf2.add(new Entry(-1f, limInf2));
        entradaLimInf2.add(new Entry(1f, limInf2));

        LineDataSet dsAlvo = makeDataSet(entradasA, "Peso Alvo", Color.GREEN, 2f, false);
        LineDataSet dsCorrigido = makeDataSet(entradasB, "Peso Corrigido", Color.BLUE, 1f, true);
        LineDataSet dsSup1 = makeDashedSet(entradaLimSup1, "+1% Limite", Color.MAGENTA);
        LineDataSet dsInf1 = makeDashedSet(entradaLimInf1, "-1% Limite", Color.MAGENTA);
        LineDataSet dsSup2 = makeDashedSet(entradaLimSup2, "+2% Limite", Color.RED);
        LineDataSet dsInf2 = makeDashedSet(entradaLimInf2, "-2% Limite", Color.RED);

        LineData lineData = new LineData(dsAlvo, dsCorrigido, dsSup1, dsInf1, dsSup2, dsInf2);
        binding.lineChart.setData(lineData);
        binding.lineChart.getDescription().setEnabled(false);

        XAxis xAxis = binding.lineChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setAxisMinimum(-1f);
        xAxis.setAxisMaximum(1f);
        xAxis.setDrawLabels(false);

        YAxis yAxis = binding.lineChart.getAxisLeft();
        yAxis.setAxisMinimum(pesoAlvo - 150);
        yAxis.setAxisMaximum(pesoAlvo + 150);

        binding.lineChart.getAxisRight().setEnabled(false);
        binding.lineChart.setTouchEnabled(false);
        binding.lineChart.animateY(800);
        binding.lineChart.invalidate();
    }

    private LineDataSet makeDataSet(ArrayList<Entry> entries, String label, int color, float width, boolean drawCircle) {
        LineDataSet ds = new LineDataSet(entries, label);
        ds.setColor(color);
        ds.setLineWidth(width);
        ds.setDrawCircles(drawCircle);
        ds.setDrawValues(drawCircle);
        if (drawCircle) ds.setCircleColor(color);
        return ds;
    }

    private LineDataSet makeDashedSet(ArrayList<Entry> entries, String label, int color) {
        LineDataSet ds = makeDataSet(entries, label, color, 1.5f, false);
        ds.enableDashedLine(10f, 10f, 0f);
        ds.setFormLineDashEffect(new DashPathEffect(new float[]{10f, 10f}, 0));
        return ds;
    }

    private double calcularPa(double altitudeFt) {
        return 1013.25 * Math.pow(1 - 6.87559e-6 * altitudeFt, 5.25588);
    }

    private double calcularK(double tempCelsius) {
        return tempCelsius + 273.15;
    }

    private double calcularAltitudeZP(double pa) {
        return (1 - Math.pow(pa / 1013.25, 1 / 5.25588)) / 6.87559e-6;
    }

    private double calcularRho(double pa, double tempKelvin, double r) {
        return pa * 100 / (r * tempKelvin);
    }

    private double calcularSigma(double rho) {
        return rho / 1.225;
    }

    private double calcularRho2(double massa , double msigma) {
        return (massa/msigma) * 1.225;
    }

    private double calcularPaux (double tar, double painic, double rho, double zpinic){
        double taux = tar;
        double roinic = (painic*100)/(Ro * tar);
        double paux = (painic * rho * tar) /(roinic * tar) ;
        double zpaux = calcularAltitudeZP(paux);
        double dzp = zpaux - zpinic;
        while (Math.abs(dzp) >= 10.0) {
            double dtemp = (2.0 * dzp) / 1000.0;
            taux -= dtemp;
            TempMeta = taux;
            double zpaux2 = zpaux;
            double paux2 = (painic * rho * taux) / (roinic * tar);
            zpaux = calcularAltitudeZP(paux2);
            dzp = zpaux - zpaux2;

        }

        double zpi = Math.round((zpaux/20)*20);
        Log.d("Paux", "ZPI: " + zpi );

        binding.textEditAltMeta.setText(String.format(Locale.US, "%.0f", zpi));
        binding.textEditTempMeta.setText(String.format(Locale.US, "%.1f", taux - 273.15));
        return paux;

    }

    private double calcularNovaTemp(double alt_fixa , double nova_alt, double temp) {
        return temp - 0.0065 * 0.3048 * (nova_alt - alt_fixa);
    }
}