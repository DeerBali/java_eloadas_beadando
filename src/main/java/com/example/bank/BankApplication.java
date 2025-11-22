package com.example.bank;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import soapclient.MNBArfolyamServiceSoap;
import soapclient.MNBArfolyamServiceSoapGetExchangeRatesStringFaultFaultMessage;
import soapclient.MNBArfolyamServiceSoapImpl;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Controller
public class BankApplication {

    @GetMapping({"/exercise"})
    public String form(Model model) {
        model.addAttribute("param", new MessagePrice());
        return "form";
    }

    @PostMapping("/exercise")
    public String submit(@ModelAttribute("param") MessagePrice messagePrice, Model model)
            throws MNBArfolyamServiceSoapGetExchangeRatesStringFaultFaultMessage {

        MNBArfolyamServiceSoap service =
                new MNBArfolyamServiceSoapImpl().getCustomBindingMNBArfolyamServiceSoap();

        String xml = service.getExchangeRates(
                messagePrice.getStartDate(),
                messagePrice.getEndDate(),
                messagePrice.getCurrency()
        );

        List<RatePoint> points = parseRates(xml, messagePrice.getCurrency());

        List<String> labels = points.stream().map(p -> p.date().toString()).collect(Collectors.toList());
        List<Double> data = points.stream().map(p -> p.value().doubleValue()).collect(Collectors.toList());

        model.addAttribute("labels", labels);
        model.addAttribute("data", data);
        model.addAttribute("points", points);
        model.addAttribute("currency", messagePrice.getCurrency());
        model.addAttribute("start", messagePrice.getStartDate());
        model.addAttribute("end", messagePrice.getEndDate());

        return "result";
    }

    private static List<RatePoint> parseRates(String innerXml, String expectedCurrency) {
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware(false);
            dbf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            dbf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            dbf.setFeature("http://xml.org/sax/features/external-general-entities", false);
            dbf.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            dbf.setExpandEntityReferences(false);

            Document doc = dbf.newDocumentBuilder()
                    .parse(new java.io.ByteArrayInputStream(innerXml.getBytes()));

            var xp = XPathFactory.newInstance().newXPath();
            NodeList days = (NodeList) xp.evaluate("/MNBExchangeRates/Day", doc, XPathConstants.NODESET);

            List<RatePoint> out = new ArrayList<>();
            for (int i = 0; i < days.getLength(); i++) {
                var day = days.item(i);
                var dateAttr = day.getAttributes().getNamedItem("date");
                if (dateAttr == null) continue;
                LocalDate date = LocalDate.parse(dateAttr.getNodeValue());

                NodeList rates = (NodeList) xp.evaluate("./Rate", day, XPathConstants.NODESET);
                for (int j = 0; j < rates.getLength(); j++) {
                    var r = rates.item(j);
                    var currNode = r.getAttributes().getNamedItem("curr");
                    if (currNode == null) currNode = r.getAttributes().getNamedItem("currency");
                    String curr = currNode != null ? currNode.getNodeValue() : "";
                    if (!expectedCurrency.equalsIgnoreCase(curr)) continue;

                    String txt = r.getTextContent().trim().replace(',', '.');
                    if (txt.isEmpty() || "-".equals(txt)) continue;

                    out.add(new RatePoint(date, new BigDecimal(txt)));
                }
            }
            out.sort(Comparator.comparing(RatePoint::date));
            return out;
        } catch (Exception e) {
            throw new RuntimeException("XML parse hiba", e);
        }
    }

    public static class MessagePrice {
        private String currency = "EUR";
        private String startDate;
        private String endDate;
        public String getCurrency() { return currency; }
        public void setCurrency(String currency) { this.currency = currency; }
        public String getStartDate() { return startDate; }
        public void setStartDate(String startDate) { this.startDate = startDate; }
        public String getEndDate() { return endDate; }
        public void setEndDate(String endDate) { this.endDate = endDate; }
    }

    public record RatePoint(LocalDate date, BigDecimal value) {}
}
