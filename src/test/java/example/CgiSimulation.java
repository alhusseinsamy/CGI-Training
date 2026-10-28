package example;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.*;

import io.gatling.javaapi.core.*;
import io.gatling.javaapi.http.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import static example.endpoints.ApiEndpoints.*;
import static example.endpoints.WebEndpoints.*;

public class CgiSimulation extends Simulation {

  private record Product(
      int id,
      String name,
      String color,
      String price,
      int quantity,
      String imageSrc,
      String imageAlt) {
  }

  private static final ObjectMapper mapper = new ObjectMapper();

  public static final FeederBuilder<Object> usersFeeder = jsonFile("users.json").circular();

  // Define HTTP configuration
  // Reference: https://docs.gatling.io/reference/script/protocols/http/protocol/
  private static final HttpProtocolBuilder httpProtocol = http.baseUrl("https://api-ecomm.gatling.io")
      .acceptHeader("application/json")
      .userAgentHeader(
          "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/134.0.0.0 Safari/537.36");

  // Define scenario
  // Reference: https://docs.gatling.io/reference/script/core/scenario/
  private static final ScenarioBuilder scenario = scenario("Scenario").exec(
      homePage,
      session,
      exec(session -> session.set("pageNumber", "0")),
      exec(session -> session.set("searchKey", "")),
      products,
      loginPage,
      feed(usersFeeder),
      login,
      products,
      exec(session -> {
        try {
          List<Product> products = mapper.readValue(
              session.getString("Products"), new TypeReference<List<Product>>() {
              });
          Random rand = new Random();
          Product randomProduct = products.get(rand.nextInt(products.size()));

          List<Product> cartItems = new ArrayList<>();
          cartItems.add(randomProduct);

          // Serialize updated cart list back to session
          String cartItemsJsonString = mapper.writeValueAsString(cartItems);
          return session.set("CartItems", cartItemsJsonString);

        } catch (Exception e) {
          throw new RuntimeException(e);

        }

      }),
      addToCart,
      checkout);

  // Define assertions
  // Reference: https://docs.gatling.io/reference/script/core/assertions/
  private static final Assertion assertion = global().failedRequests().count().lt(1L);

  // Define injection profile and execute the test
  // Reference: https://docs.gatling.io/reference/script/core/injection/
  {
    setUp(scenario.injectOpen(atOnceUsers(1))).assertions(assertion).protocols(httpProtocol);
  }
}
