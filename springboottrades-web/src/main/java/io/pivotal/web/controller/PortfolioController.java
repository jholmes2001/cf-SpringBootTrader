package io.pivotal.web.controller;

import java.util.Calendar;

import javax.servlet.http.HttpServletRequest;

import io.pivotal.web.domain.Order;
import io.pivotal.web.domain.Search;
import io.pivotal.web.service.MarketService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.servlet.ModelAndView;

import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;

@Controller
public class PortfolioController {
	private static final Logger logger = LoggerFactory
			.getLogger(PortfolioController.class);
	
	@Autowired
	private MarketService marketService;
	@RequestMapping(value = "/portfolio", method = RequestMethod.GET)
	public String portfolio(Model model) {
		logger.debug("/portfolio");
		model.addAttribute("marketSummary", marketService.getMarketSummary());
		
		//check if user is logged in!
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (!(authentication instanceof AnonymousAuthenticationToken)) {
		    String currentUserName = authentication.getName();
		    logger.debug("portfolio: User logged in: " + currentUserName);
		    
		    //TODO: add account summary.
		    try {
		    	model.addAttribute("portfolio",marketService.getPortfolio(currentUserName));
		    } catch (HttpServerErrorException e) {
		    	logger.debug("error retrieving portfolfio: " + e.getMessage());
		    	model.addAttribute("portfolioRetrievalError",e.getMessage());
		    }
		    model.addAttribute("order", new Order());
		}
		
		return "portfolio";
	}
	
    @Autowired
    private FlakeyService service;

    @RequestMapping("/cbtest")
    public String hello() {
        return service.hello();
    }

    @Component
    public static class FlakeyService {

        @HystrixCommand(fallbackMethod="goodbye")
        public String hello() {
            if (Calendar.getInstance().get(Calendar.MINUTE) % 2 == 0) {
                throw new RuntimeException();
            }
            return "hello!";
        }

        String goodbye() {
            return "goodbye.";
        }

    }
			
	@ExceptionHandler({ Exception.class })
	public ModelAndView error(HttpServletRequest req, Exception exception) {
		logger.debug("Handling error: " + exception);
		ModelAndView model = new ModelAndView();
		model.addObject("errorCode", exception.getMessage());
		model.addObject("errorMessage", exception);
		model.setViewName("error");
		exception.printStackTrace();
		return model;
	}

}
