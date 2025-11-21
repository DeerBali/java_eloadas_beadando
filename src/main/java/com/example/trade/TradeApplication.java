package com.example.trade;
import static com.oanda.v20.instrument.CandlestickGranularity.*;
import com.oanda.v20.Context;
import com.oanda.v20.account.AccountSummary;
import com.oanda.v20.pricing.ClientPrice;
import com.oanda.v20.pricing.PricingGetRequest;
import com.oanda.v20.pricing.PricingGetResponse;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import com.oanda.v20.instrument.Candlestick;
import com.oanda.v20.instrument.InstrumentCandlesRequest;
import com.oanda.v20.instrument.InstrumentCandlesResponse;
import com.oanda.v20.primitives.InstrumentName;
import static com.oanda.v20.instrument.CandlestickGranularity.H1;
import org.springframework.ui.Model;
import java.util.ArrayList;
import java.util.List;
import com.oanda.v20.order.MarketOrderRequest;
import com.oanda.v20.order.OrderCreateRequest;
import com.oanda.v20.order.OrderCreateResponse;
import com.oanda.v20.trade.Trade;

import com.oanda.v20.trade.*;



@SpringBootApplication
@Controller
public class TradeApplication {
    public static void main(String[] args) {
        SpringApplication.run(TradeApplication.class, args);
    }

    @GetMapping("/account_info")
    @ResponseBody
    public AccountSummary f1() {
        Context ctx = new Context(Config.URL,Config.TOKEN);
        try {
            AccountSummary summary = ctx.account.summary(Config.ACCOUNTID).getAccount();
            return summary;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @GetMapping("/actual_prices")
    public String actual_prices(Model model) {
        model.addAttribute("par", new MessageActPrice());
        return "form_actual_prices";
    }

    @PostMapping("/actual_prices")
    public String actual_prices2(@ModelAttribute MessageActPrice messageActPrice, Model model) {
        String strOut="";
        List<String> instruments = new ArrayList<>( );
        instruments.add(messageActPrice.getInstrument());
        try {
            PricingGetRequest request = new PricingGetRequest(Config.ACCOUNTID, instruments);
            Context ctx = new Context(Config.URL,Config.TOKEN);
            PricingGetResponse resp = ctx.pricing.get(request);
            for (ClientPrice price : resp.getPrices())
                strOut+=price+"<br>";
        } catch (Exception e) {
            throw new RuntimeException(e);
        } model.addAttribute("instr", messageActPrice.getInstrument());
        model.addAttribute("price", strOut);
        return "result_actual_prices";
    }

    @GetMapping("/hist_prices")
    public String hist_prices(Model model) {
        model.addAttribute("param", new MessageHistPrice()); return "form_hist_prices";
    }

    @PostMapping("/hist_prices")
    public String hist_prices2(@ModelAttribute MessageHistPrice messageHistPrice, Model model) {
        String strOut;
        Context ctx = new Context(Config.URL,Config.TOKEN);
     try {
        InstrumentCandlesRequest request = new InstrumentCandlesRequest(new InstrumentName(messageHistPrice.getInstrument()));
        switch (messageHistPrice.getGranularity()) {
            case "M1": request.setGranularity(M1); break;
            case "H1": request.setGranularity(H1); break;
            case "D": request.setGranularity(D); break;
            case "W": request.setGranularity(W); break;
            case "M": request.setGranularity(M); break;
        }
    request.setCount(Long.valueOf(10)); // utolsó 10 árat kérjünk
        InstrumentCandlesResponse resp = ctx.instrument.candles(request);
        strOut = "";
        for (Candlestick candle : resp.getCandles())
            strOut += candle.getTime() + "\t" + candle.getMid().getC() + ";";
    } catch (Exception e) {
        throw new RuntimeException(e);
    }
    model.addAttribute("instr", messageHistPrice.getInstrument());
    model.addAttribute("granularity", messageHistPrice.getGranularity());
    model.addAttribute("price", strOut);
    return "result_hist_prices";
    }


    @GetMapping("/open_position")
    public String open_position(Model model) {
        model.addAttribute("param", new MessageOpenPosition());
        return "form_open_position";
    }

    @PostMapping("/open_position")
    public String open_position2(@ModelAttribute MessageOpenPosition messageOpenPosition, Model model) {
        String strOut;
        Context ctx = new Context(Config.URL,Config.TOKEN);
        try {
            InstrumentName instrument = new InstrumentName(messageOpenPosition.getInstrument());
            OrderCreateRequest request = new OrderCreateRequest(Config.ACCOUNTID);
            MarketOrderRequest marketorderrequest = new MarketOrderRequest();
            marketorderrequest.setInstrument(instrument);
            marketorderrequest.setUnits(messageOpenPosition.getUnits());
            request.setOrder(marketorderrequest);
            OrderCreateResponse response = ctx.order.create(request);
            strOut="tradeId: "+response.getOrderFillTransaction().getId();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        model.addAttribute("instr", messageOpenPosition.getInstrument());
        model.addAttribute("units", messageOpenPosition.getUnits());
        model.addAttribute("id", strOut);
        return "result_open_position";
    }

    @GetMapping("/positions")
    @ResponseBody
    public String positions() {
        Context ctx = new Context(Config.URL,Config.TOKEN);
        String strOut="Open positions:<br>";
        try {
            List<Trade> trades = ctx.trade.listOpen(Config.ACCOUNTID).getTrades();
            for(Trade trade: trades) strOut+=trade.getId()+","+trade.getInstrument()+", "+trade.getOpenTime()+", "+trade.getCurrentUnits()+", "+trade.getPrice()+", "+trade.getUnrealizedPL()+"<br>";
            return strOut;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @GetMapping("/close_position")
    public String close_position(Model model) {
        model.addAttribute("param", new MessageClosePosition());
        return "form_close_position";
    }

    @PostMapping("/close_position")
    public String close_position2(@ModelAttribute MessageClosePosition messageClosePosition, Model model) {
        String tradeId= messageClosePosition.getTradeId()+"";
        String strOut="Closed tradeId= "+tradeId;
        Context ctx = new Context(Config.URL,Config.TOKEN);
        try {
            ctx.trade.close(new TradeCloseRequest(Config.ACCOUNTID, new TradeSpecifier(tradeId)));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        model.addAttribute("tradeId", strOut);
        return "result_close_position";
    }

    @GetMapping("/pelda")
    public String pelda() {
        return "pelda";
    }



}
