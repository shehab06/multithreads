package Services;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import Entity.*;

public class OrderService extends EntityService<Order> {
    private PermissionService permission;
    private ProductService productService;
    private CustomerService customerService;

    public OrderService(AuthService authService, PermissionService permission, ProductService productService,
            CustomerService customerService) {
        super("orders", Order.class, authService);
        this.permission = permission;
        this.productService = productService;
        this.customerService = customerService;
    }

    public PermissionService getPermission() {
        return permission;
    }

    public void setPermission(PermissionService permission) {
        this.permission = permission;
    }

    public ProductService getProductService() {
        return productService;
    }

    public void setProductService(ProductService productService) {
        this.productService = productService;
    }

    public CustomerService getCustomerService() {
        return customerService;
    }

    public void setCustomerService(CustomerService customerService) {
        this.customerService = customerService;
    }

    public double calcTotal(Map<String, Integer> products) {
        Product product;
        double Total = 0;
        for (Map.Entry<String, Integer> entry : products.entrySet()) {
            product = productService.get(entry.getKey());
            Total += (product.getPrice() * entry.getValue());
        }
        return Total;
    }

    public void addItem(String OrderID, String ProductID) {
        if (permission.hasPermission("orders", "update")) {
            Order order = getEntityDAO().get(OrderID);
            if (order == null) {
                throw new IllegalArgumentException("This order doesn't exist.");
            }
            order.addProduct(ProductID);
            order.setProducts(order.getProducts());
            order.setTotal(calcTotal(order.getProducts()));
            getEntityDAO().update(order);
        } else {
            throw new RuntimeException("You don't have the permisson to do this action");
        }
    }

    public void deleteItem(String OrderID, String ProductID) {
        if (permission.hasPermission("orders", "update")) {
            Order order = getEntityDAO().get(OrderID);
            if (order == null) {
                throw new IllegalArgumentException("This order doesn't exist.");
            }
            order.removeProduct(ProductID);
            order.setProducts(order.getProducts());
            order.setTotal(calcTotal(order.getProducts()));
            getEntityDAO().update(order);

        } else {
            throw new RuntimeException("You don't have the permisson to do this action");
        }
    }

    public String create(String customer, Map<String, Integer> products, PaymentMethod paymentMethod, Status status) {
        if ((getLoggedInUser() == null && status.equals(Status.draft))) {
            String id = getEntityDAO().nextId();
            getEntityDAO().add(new Order(id, customer, products, calcTotal(products), paymentMethod, status));
            return id;
        }

        if (permission.hasPermission("orders", "create")) {
            if (customerService.get(customer) == null) {
                throw new IllegalArgumentException("This customer doesn't exist");
            }
            String id = getEntityDAO().nextId();
            getEntityDAO().add(new Order(id, customer, products, calcTotal(products), paymentMethod, status));
            return id;
        } else {
            throw new RuntimeException("You don't have the permisson to do this action");
        }
    }

    public void delete(String OrderID) {
        if (permission.hasPermission("orders", "delete")) {
            getEntityDAO().delete(OrderID);
        } else {
            throw new RuntimeException("You don't have the permisson to do this action");
        }
    }

    public Order get(String OrderID) {
        if (permission.hasPermission("orders", "retrieve")) {
            Order order = getEntityDAO().get(OrderID);
            if (order == null) {
                throw null;
            }
            return new Order(order);
        } else {
            throw new RuntimeException("You don't have the permisson to do this action");
        }
    }

    public <T> ArrayList<Order> filter(String paramater, T data) {
        if (permission.hasPermission("orders", "retrieve")) {
            ArrayList<Order> orders = new ArrayList<>();
            if (!(paramater.toLowerCase() == "customer" || paramater.toLowerCase() == "product"
                    || paramater.toLowerCase() == "paymentmethod" || paramater.toLowerCase() == "status")) {
                throw new IllegalArgumentException("This parameter doesn't exist");
            }

            for (Order order : getEntityDAO().getAll()) {
                if (order.getCustomer() != null && order.getCustomer().equals(data)
                        && paramater.toLowerCase() == "customer") {
                    orders.add(order);
                } else if (order.getProducts() != null && order.getProducts().containsKey(data)
                        && paramater.toLowerCase() == "product") {
                    orders.add(order);
                } else if (order.getPaymentMethod() != null && order.getPaymentMethod().equals(data)
                        && paramater.toLowerCase() == "paymentmethod") {
                    orders.add(order);
                } else if (order.getStatus() != null && order.getStatus().equals(data)
                        && paramater.toLowerCase() == "status") {
                    orders.add(order);
                }
            }
            return orders;

        } else {
            throw new RuntimeException("You don't have the permisson to do this action");
        }
    }

    public ArrayList<Order> getAll() {
        if (permission.hasPermission("orders", "retrieve")) {
            return getEntityDAO().getAll();
        } else {
            throw new RuntimeException("You don't have the permisson to do this action");
        }
    }

    public String convertToOrder(String OrderID) {
        Order cart = getEntityDAO().get(OrderID);
        if (cart == null) {
            throw new IllegalArgumentException("This cart doesn't exist.");
        }
        if (permission.hasPermission("orders", "update")
                || cart.getCustomer().equals(getLoggedInUser().getUsername())) {
            if (cart.getStatus().equals(Status.draft)) {
                if (cart.getProducts().isEmpty()) {
                    throw new RuntimeException("This cart is Empty.");
                }
                Order order = new Order(cart);
                cart.setProducts(new HashMap<>());
                cart.setTotal(0);
                order.setId(getEntityDAO().nextId());
                order.setStatus(Status.processing);
                System.out.println(order.getId());
                getEntityDAO().update(cart);
                getEntityDAO().add(order);
                return order.getId();
            } else {
                throw new RuntimeException("This is already an order.");
            }

        } else {
            throw new RuntimeException("You don't have the permisson to do this action");
        }
    }

    public void pay(String OrderID, PaymentMethod payMeth) throws RuntimeException {
        Order order = getEntityDAO().get(OrderID);
        if (order == null) {
            throw new IllegalArgumentException("this order doesn't exist.");
        }

        if (permission.hasPermission("orders", "update")
                || order.getCustomer().equals(getLoggedInUser().getUsername())) {
            if (order.getProducts().isEmpty()) {
                throw new RuntimeException("This order is Empty.");
            }

            if (order.getStatus().equals(Status.processing)) {
                order.setPaymentMethod(payMeth);
                Customer customer = customerService.get(order.getCustomer());
                customerService.update(order.getCustomer(), "balance", customer.getBalance() - order.getTotal());
                order.setStatus(Status.shipping);
                getEntityDAO().update(order);
            } else
                throw new RuntimeException("Can't pay for this order");

        } else {
            throw new RuntimeException("You don't have the permisson to do this action");
        }
    }

    public void deliver(String OrderID) {
        Order order = getEntityDAO().get(OrderID);
        if (order == null) {
            throw new IllegalArgumentException("this order doesn't exist.");
        }

        if (permission.hasPermission("orders", "update")
                || order.getCustomer().equals(getLoggedInUser().getUsername())) {
            if (order.getStatus().equals(Status.shipping)) {
                order.setStatus(Status.delivered);
                getEntityDAO().update(order);
            } else {
                throw new RuntimeException("This order hasn't been shipped.");
            }

        } else {
            throw new RuntimeException("You don't have the permisson to do this action");
        }
    }

    public void close(String OrderID) {
        Order order = getEntityDAO().get(OrderID);
        if (order == null) {
            throw new IllegalArgumentException("this order doesn't exist.");
        }

        if (permission.hasPermission("orders", "update")
                || order.getCustomer().equals(getLoggedInUser().getUsername())) {
            if (order.getStatus().equals(Status.delivered)) {
                order.setStatus(Status.closed);
                getEntityDAO().update(order);
            } else {
                throw new RuntimeException("This order hasn't been delivered.");
            }
        } else {
            throw new RuntimeException("You don't have the permisson to do this action");
        }
    }

    public void cancel(String OrderID) {
        Order order = getEntityDAO().get(OrderID);
        if (order == null) {
            throw new IllegalArgumentException("this order doesn't exist.");
        }

        if (permission.hasPermission("orders", "update")
                || order.getCustomer().equals(getLoggedInUser().getUsername())) {
            if (!(order.getStatus().equals(Status.closed) || order.getStatus().equals(Status.draft)
                    || order.getStatus().equals(Status.cancelled))) {
                order.setStatus(Status.cancelled);
                getEntityDAO().update(order);
            } else {
                throw new RuntimeException("You can't cancel this order.");
            }
        } else {
            throw new RuntimeException("You don't have the permisson to do this action");
        }
    }

    public void setStatus(String OrderID, Status status) {
        if (permission.hasPermission("orders", "update") && (getLoggedInUser() instanceof Admin)) {
            Order order = getEntityDAO().get(OrderID);
            if (order == null) {
                throw new IllegalArgumentException("this order doesn't exist.");
            }

            order.setStatus(status);
            getEntityDAO().update(order);
        } else {
            throw new RuntimeException("You don't have the permisson to do this action");
        }

    }

}