package com.example.trade;

import static com.oanda.v20.instrument.CandlestickGranularity.*;

import com.oanda.v20.Context;
import com.oanda.v20.account.AccountSummary;
import com.oanda.v20.instrument.Candlestick;
import com.oanda.v20.instrument.InstrumentCandlesRequest;
import com.oanda.v20.instrument.InstrumentCandlesResponse;
import com.oanda.v20.order.*;
import com.oanda.v20.pricing.ClientPrice;
import com.oanda.v20.pricing.PricingGetRequest;
import com.oanda.v20.pricing.PricingGetResponse;
import com.oanda.v20.primitives.InstrumentName;
import com.oanda.v20.trade.Trade;
import com.oanda.v20.trade.TradeCloseRequest;
import com.oanda.v20.trade.TradeSpecifier;


import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@Controller
public class TradeApplication {

    @GetMapping("/account_info")
    @ResponseBody
    public AccountSummary f1() {
        Context ctx = new Context(Config.URL, Config.TOKEN);
        try {
            return ctx.account.summary(Config.ACCOUNTID).getAccount();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // ------------------------------------------------------
    // ACTUAL PRICES
    // ------------------------------------------------------

    @GetMapping("/actual_prices")
    public String actual_prices(Model model) {
        model.addAttribute("par", new MessageActPrice());
        return "form_actual_prices";
    }

    @PostMapping("/actual_prices")
    public String actual_prices2(
            @ModelAttribute MessageActPrice messageActPrice,
            Model model
    ) {

        try {
            Context ctx = new Context(Config.URL, Config.TOKEN);

            List<String> instruments = List.of(messageActPrice.getInstrument());
            PricingGetRequest request = new PricingGetRequest(Config.ACCOUNTID, instruments);
            PricingGetResponse resp = ctx.pricing.get(request);

            ClientPrice cp = resp.getPrices().get(0);   // → csak egy instrumentet kérünk

            // ✔️ Szétszedett adatok template-nek
            model.addAttribute("instr", cp.getInstrument());
            model.addAttribute("time", cp.getTime().toString());
            model.addAttribute("tradeable", cp.getTradeable());

            model.addAttribute("bids", cp.getBids());
            model.addAttribute("asks", cp.getAsks());

            model.addAttribute("closeoutBid", cp.getCloseoutBid());
            model.addAttribute("closeoutAsk", cp.getCloseoutAsk());

        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return "result_actual_prices";
    }


    // ------------------------------------------------------
    // HISTORICAL PRICES
    // ------------------------------------------------------

    @GetMapping("/hist_prices")
    public String hist_prices(Model model) {
        model.addAttribute("param", new MessageHistPrice());
        return "form_hist_prices";
    }

    @PostMapping("/hist_prices")
    public String hist_prices2(@ModelAttribute MessageHistPrice messageHistPrice,
                               Model model) {

        Context ctx = new Context(Config.URL, Config.TOKEN);

        StringBuilder sb = new StringBuilder();

        try {
            InstrumentCandlesRequest request =
                    new InstrumentCandlesRequest(new InstrumentName(messageHistPrice.getInstrument()));

            switch (messageHistPrice.getGranularity()) {
                case "M1": request.setGranularity(M1); break;
                case "H1": request.setGranularity(H1); break;
                case "D":  request.setGranularity(D); break;
                case "W":  request.setGranularity(W); break;
                case "M":  request.setGranularity(M); break;
            }

            request.setCount(10L);

            InstrumentCandlesResponse resp = ctx.instrument.candles(request);

            for (Candlestick candle : resp.getCandles()) {
                sb.append(candle.getTime())
                        .append(" ")
                        .append(candle.getMid().getC())
                        .append(";");
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        List<String> rows = new ArrayList<>();

        for (String row : sb.toString().split(";")) {
            if (!row.trim().isEmpty()) {
                rows.add(row.trim());
            }
        }

        model.addAttribute("instr", messageHistPrice.getInstrument());
        model.addAttribute("granularity", messageHistPrice.getGranularity());
        model.addAttribute("prices", rows);

        return "result_hist_prices";
    }

    // ------------------------------------------------------
    // OPEN POSITION
    // ------------------------------------------------------

    @GetMapping("/open_position")
    public String open_position(Model model) {
        model.addAttribute("param", new MessageOpenPosition());
        return "form_open_position";
    }

    @PostMapping("/open_position")
    public String open_position2(
            @ModelAttribute MessageOpenPosition messageOpenPosition,
            Model model) {

        String strOut;

        Context ctx = new Context(Config.URL, Config.TOKEN);

        try {
            InstrumentName instrument =
                    new InstrumentName(messageOpenPosition.getInstrument());

            OrderCreateRequest request =
                    new OrderCreateRequest(Config.ACCOUNTID);

            MarketOrderRequest marketorderrequest =
                    new MarketOrderRequest();

            marketorderrequest.setInstrument(instrument);
            marketorderrequest.setUnits(messageOpenPosition.getUnits());

            request.setOrder(marketorderrequest);

            OrderCreateResponse response = ctx.order.create(request);

            // --------- HIBAMENTES TRADE ID KEZELÉS ----------
            if (response.getOrderFillTransaction() != null) {
                strOut = "tradeId: " +
                        response.getOrderFillTransaction().getId();
            } else {
                // hétvégén / zárt piacon ide esik
                strOut = "A pozíció nem nyílt meg (valószínűleg zárt a piac).";
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        model.addAttribute("instr", messageOpenPosition.getInstrument());
        model.addAttribute("units", messageOpenPosition.getUnits());
        model.addAttribute("id", strOut);

        return "result_open_position";
    }


    // ------------------------------------------------------
    // LIST OPEN POSITIONS
    // ------------------------------------------------------

    @GetMapping("/forex-positions")
    public String forexPositions(Model model) {

        Context ctx = new Context(Config.URL, Config.TOKEN);

        List<Trade> trades;

        try {
            trades = ctx.trade.listOpen(Config.ACCOUNTID).getTrades();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        model.addAttribute("trades", trades);
        return "forex_positions";
    }

    // ------------------------------------------------------
    // CLOSE POSITION
    // ------------------------------------------------------

    @GetMapping("/close_position")
    public String close_position(Model model) {
        model.addAttribute("param", new MessageClosePosition());
        return "form_close_position";
    }

    @PostMapping("/close_position")
    public String close_position2(
            @ModelAttribute MessageClosePosition messageClosePosition,
            Model model) {

        String tradeId = String.valueOf(messageClosePosition.getTradeId());
        String strOut = "Closed tradeId = " + tradeId;

        Context ctx = new Context(Config.URL, Config.TOKEN);

        try {

            TradeCloseRequest req = new TradeCloseRequest(
                    Config.ACCOUNTID,
                    new TradeSpecifier(tradeId)
            );

            // **Fontos: minden egységet zárjon**
            req.setUnits("ALL");

            ctx.trade.close(req);

        } catch (Exception e) {
            // ha hiba jön, látni akarjuk
            e.printStackTrace();
            model.addAttribute("error", e.getMessage());
            return "error_page";
        }

        model.addAttribute("tradeId", strOut);
        return "result_close_position";
    }


    @GetMapping("/forex-account")
    public String forexAccount(Model model) {

        Context ctx = new Context(Config.URL, Config.TOKEN);

        AccountSummary summary;

        try {
            summary = ctx.account.summary(Config.ACCOUNTID).getAccount();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        model.addAttribute("acc", summary);
        return "forex_account";
    }
}
