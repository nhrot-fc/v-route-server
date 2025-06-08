package com.example.plgsystem.repository;

import com.example.plgsystem.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, String> {
    
    // Find orders by delivery status
    @Query("SELECT o FROM Order o WHERE o.remainingGLP > 0")
    List<Order> findPendingOrders();
    
    @Query("SELECT o FROM Order o WHERE o.remainingGLP = 0")
    List<Order> findCompletedOrders();
    
    // Find orders by date range
    @Query("SELECT o FROM Order o WHERE o.arriveDate BETWEEN :startDate AND :endDate")
    List<Order> findOrdersByArrivalDateRange(@Param("startDate") LocalDateTime startDate, 
                                           @Param("endDate") LocalDateTime endDate);
    
    // Find overdue orders
    @Query("SELECT o FROM Order o WHERE o.dueDate < :currentTime AND o.remainingGLP > 0")
    List<Order> findOverdueOrders(@Param("currentTime") LocalDateTime currentTime);
    
    // Find urgent orders (due within specified hours)
    @Query("SELECT o FROM Order o WHERE o.dueDate <= :deadline AND o.remainingGLP > 0 ORDER BY o.dueDate ASC")
    List<Order> findUrgentOrders(@Param("deadline") LocalDateTime deadline);
    
    // Additional methods for test compatibility
    @Query("SELECT o FROM Order o WHERE o.dueDate BETWEEN :startDate AND :endDate")
    List<Order> findOrdersByDueDate(@Param("startDate") LocalDateTime startDate, 
                                  @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT o FROM Order o WHERE o.arriveDate BETWEEN :startDate AND :endDate")
    List<Order> findOrdersByDateRange(@Param("startDate") LocalDateTime startDate, 
                                    @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT o FROM Order o WHERE " +
           "sqrt(power(o.position.x - :centerX, 2) + power(o.position.y - :centerY, 2)) <= :radius")
    List<Order> findOrdersByRadius(@Param("centerX") int centerX, 
                                 @Param("centerY") int centerY, 
                                 @Param("radius") double radius);
}
