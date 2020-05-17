package com.space.service;

import com.space.controller.ShipOrder;
import com.space.exception.ShipBadRequestException;
import com.space.exception.ShipNotFoundException;
import com.space.model.Ship;
import com.space.repository.ShipRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Service
public class ShipServiceImpl implements ShipService {

    private static final Calendar YEAR2800 = new GregorianCalendar(2800, 1, 1);
    private static final Calendar YEAR3019 = new GregorianCalendar(3019, 1, 1);

    @PersistenceContext
    private EntityManager em;

    @Autowired
    private ShipRepository shipRepository;

    @Override
    public List<Ship> getShips(ShipDTO shipDTO) {
        return makeRequestInDB(shipDTO);
    }

    @Override
    public Ship getShip(Long id) {
        if (isValidId(id)) {
            return shipRepository.findById(id).orElseThrow(() -> new ShipNotFoundException());
        }
        throw new ShipBadRequestException();
    }

    @Override
    public Ship createShip(Ship ship) {
        if (isValidName(ship.getName()) && isValidName(ship.getPlanet())
                && isValidName(ship.getShipType()) && isValidProdDate(ship.getProdDate().getTime())
                && isValidSpeed(ship.getSpeed()) && isValidCrewSize(ship.getCrewSize())) {
            if (ship.isUsed() == null) {
                ship.setUsed(false);
            }
            ship.setRating(getMathRating(ship.isUsed(), ship.getSpeed(), ship.getProdDate()));
            ship.setSpeed(getMathSpeed(ship.getSpeed()));
            shipRepository.saveAndFlush(ship);
            return ship;
        } else {
            throw new ShipBadRequestException();
        }
    }

    @Override
    public Ship updateShip(Long id, Ship requestShip) {
        if (isValidId(id)) {
            Ship ship = shipRepository.findById(id).orElseThrow(() -> new ShipNotFoundException());

            if (requestShip.getName() != null) {
                if (isValidName(requestShip.getName())) {
                    ship.setName(requestShip.getName());
                } else {
                    throw new ShipBadRequestException();
                }
            }

            if (requestShip.getSpeed() != null) {
                if (isValidSpeed(requestShip.getSpeed())) {
                    ship.setSpeed(requestShip.getSpeed());
                } else {
                    throw new ShipBadRequestException();
                }
            }

            if (requestShip.getCrewSize() != null) {
                if (isValidCrewSize(requestShip.getCrewSize())) {
                    ship.setCrewSize(requestShip.getCrewSize());
                } else {
                    throw new ShipBadRequestException();
                }
            }


            if (requestShip.isUsed() != null) {
                ship.setUsed(requestShip.isUsed());
            }

            if (requestShip.getShipType() != null) {
                if (isValidName(requestShip.getShipType())) {
                    ship.setShipType(requestShip.getShipType());
                } else {
                    throw new ShipBadRequestException();
                }
            }

            if (requestShip.getPlanet() != null) {
                if (isValidName(requestShip.getPlanet())) {
                    ship.setPlanet(requestShip.getPlanet());
                } else {
                    throw new ShipBadRequestException();
                }
            }

            if (requestShip.getProdDate() != null) {
                if (isValidProdDate(requestShip.getProdDate().getTime())) {
                    ship.setProdDate(requestShip.getProdDate());
                } else {
                    throw new ShipBadRequestException();
                }
            }

            ship.setRating(getMathRating(ship.isUsed(), ship.getSpeed(), ship.getProdDate()));
            shipRepository.saveAndFlush(ship);
            return ship;
        }
        return null;
    }

    @Override
    public Long shipCount(ShipDTO shipDTO) {
        return (long) makeRequestInDB(shipDTO).size();
    }

    @Override
    public void deleteShip(Long id) {
        if (isValidId(id)) {
            Ship ship = shipRepository.findById(id).orElseThrow(() -> new ShipNotFoundException());
            shipRepository.delete(ship);
        }
    }

    private List<Ship> makeRequestInDB(ShipDTO shipDTO) {
        Optional<String> dtoName = shipDTO.getName();
        Optional<String> dtoPlanet = shipDTO.getPlanet();
        Optional<String> dtoShipType = shipDTO.getShipType();
        Optional<Long> dtoProdDateBefore = shipDTO.getBefore();
        Optional<Long> dtoProdDateAfter = shipDTO.getAfter();
        Optional<Boolean> dtoIsUsed = shipDTO.getIsUsed();
        Optional<Double> dtoSpeedMin = shipDTO.getMinSpeed();
        Optional<Double> dtoSpeedMax = shipDTO.getMaxSpeed();
        Optional<Integer> dtoCrewSizeMin = shipDTO.getMinCrewSize();
        Optional<Integer> dtoCrewSizeMax = shipDTO.getMaxCrewSize();
        Optional<Double> dtoRatingMin = shipDTO.getMinRating();
        Optional<Double> dtoRatingMax = shipDTO.getMaxRating();
        Optional<String> dtoOrder = shipDTO.getOrder();
        Optional<Integer> dtoPageNumber = shipDTO.getPageNumber();
        Optional<Integer> dtoPageSize = shipDTO.getPageSize();

        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Ship> ShipCriteria = cb.createQuery(Ship.class);
        Root<Ship> ShipRoot = ShipCriteria.from(Ship.class);
        ShipCriteria.select(ShipRoot);
        Predicate criteria = cb.conjunction();
        if (dtoName.isPresent() && isValidName(dtoName.get())) {
            Predicate p = cb.like(ShipRoot.get("name"), "%" + dtoName.get() + "%");
            criteria = cb.and(criteria, p);
        }

        if (dtoPlanet.isPresent() && isValidName(dtoPlanet.get())
        ) {
            Predicate p = cb.like(ShipRoot.get("planet"), "%" + dtoPlanet.get() + "%");
            criteria = cb.and(criteria, p);
        }

        if (dtoShipType.isPresent()) {
            Predicate p = cb.equal(ShipRoot.get("shipType"), dtoShipType.get());
            criteria = cb.and(criteria, p);
        }

        if (dtoProdDateAfter.isPresent() && isValidProdDate(dtoProdDateAfter.get())
                && dtoProdDateBefore.isPresent() && isValidProdDate(dtoProdDateBefore.get())) {
            Predicate p = cb.between(ShipRoot.get("prodDate"), new java.sql.Date(dtoProdDateAfter.get()), new java.sql.Date(dtoProdDateBefore.get()));
            criteria = cb.and(criteria, p);
        } else if (dtoProdDateAfter.isPresent() && isValidProdDate(dtoProdDateAfter.get())) {
            Predicate p = cb.greaterThanOrEqualTo(ShipRoot.get("prodDate"), new java.sql.Date(dtoProdDateAfter.get()));
            criteria = cb.and(criteria, p);
        } else if (dtoProdDateBefore.isPresent() && isValidProdDate(dtoProdDateBefore.get())) {
            Predicate p = cb.lessThanOrEqualTo(ShipRoot.get("prodDate"), new java.sql.Date(dtoProdDateBefore.get()));
            criteria = cb.and(criteria, p);
        }

        if (dtoIsUsed.isPresent()) {
            Predicate p = cb.equal(ShipRoot.get("isUsed"), boolToInteger(dtoIsUsed.get()));
            criteria = cb.and(criteria, p);
        }

        if (dtoCrewSizeMin.isPresent() && isValidCrewSize(dtoCrewSizeMin.get())
                && dtoCrewSizeMax.isPresent() && isValidCrewSize(dtoCrewSizeMax.get())) {
            Predicate p = cb.between(ShipRoot.get("crewSize"), dtoCrewSizeMin.get(), dtoCrewSizeMax.get());
            criteria = cb.and(criteria, p);
        } else if (dtoCrewSizeMin.isPresent() && isValidCrewSize(dtoCrewSizeMin.get())) {
            Predicate p = cb.greaterThanOrEqualTo(ShipRoot.get("crewSize"), dtoCrewSizeMin.get());
            criteria = cb.and(criteria, p);
        } else if (dtoCrewSizeMax.isPresent() && isValidCrewSize(dtoCrewSizeMax.get())) {
            Predicate p = cb.lessThanOrEqualTo(ShipRoot.get("crewSize"), dtoCrewSizeMax.get());
            criteria = cb.and(criteria, p);
        }

        if (dtoSpeedMin.isPresent() && isValidSpeed(dtoSpeedMin.get())
                && dtoSpeedMax.isPresent() && isValidSpeed(dtoSpeedMax.get())) {
            Predicate p = cb.between(ShipRoot.get("speed"), dtoSpeedMin.get(), dtoSpeedMax.get());
            criteria = cb.and(criteria, p);
        } else if (dtoSpeedMin.isPresent() && isValidSpeed(dtoSpeedMin.get())) {
            Predicate p = cb.greaterThanOrEqualTo(ShipRoot.get("speed"), dtoSpeedMin.get());
            criteria = cb.and(criteria, p);
        } else if (dtoSpeedMax.isPresent() && isValidSpeed(dtoSpeedMax.get())) {
            Predicate p = cb.lessThanOrEqualTo(ShipRoot.get("speed"), dtoSpeedMax.get());
            criteria = cb.and(criteria, p);
        }

        if (dtoRatingMin.isPresent() && isValidRating(dtoRatingMin.get())
                && dtoRatingMax.isPresent() && isValidRating(dtoRatingMax.get())) {
            Predicate p = cb.between(ShipRoot.get("rating"), dtoRatingMin.get(), dtoRatingMax.get());
            criteria = cb.and(criteria, p);
        } else if (dtoRatingMin.isPresent() && isValidRating(dtoRatingMin.get())) {
            Predicate p = cb.greaterThanOrEqualTo(ShipRoot.get("rating"), dtoRatingMin.get());
            criteria = cb.and(criteria, p);
        } else if (dtoRatingMax.isPresent() && isValidRating(dtoRatingMax.get())) {
            Predicate p = cb.lessThanOrEqualTo(ShipRoot.get("rating"), dtoRatingMax.get());
            criteria = cb.and(criteria, p);
        }

        ShipCriteria.where(criteria);

        if (dtoOrder.isPresent() && isValidOrder(dtoOrder.get())) {
            ShipCriteria.orderBy(cb.asc(ShipRoot.get(ShipOrder.valueOf(dtoOrder.get()).getFieldName())));
        }

        if (dtoPageSize.isPresent() && isValidPageSize(dtoPageSize.get())
                && dtoPageNumber.isPresent() && isValidPageNumber(dtoPageNumber.get())
                && dtoPageNumber.get() == 0) {
            return em.createQuery(ShipCriteria).setFirstResult(dtoPageNumber.get()).setMaxResults(dtoPageSize.get()).getResultList();
        } else if (dtoPageSize.isPresent() && isValidPageSize(dtoPageSize.get())
                && dtoPageNumber.isPresent() && isValidPageNumber(dtoPageNumber.get())) {
            return em.createQuery(ShipCriteria).setFirstResult(dtoPageNumber.get() * dtoPageSize.get()).setMaxResults(dtoPageSize.get()).getResultList();
        } else {
            return em.createQuery(ShipCriteria).getResultList();
        }
    }

    private double getMathSpeed(double shipSpeed) {
        return new BigDecimal(shipSpeed).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    private double getMathRating(Boolean isUsed, double shipSpeed, Date prodDate) {
        double usedRating = isUsed ? 0.5 : 1;
        double prt1 = 80 * shipSpeed * usedRating;
        double prt2 = YEAR3019.getTime().getYear() - prodDate.getYear() + 1;
        double prt3 = prt1 / prt2;
        return new BigDecimal(prt3).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    private int boolToInteger(boolean isUsed) {
        return isUsed ? 1 : 0;
    }

    private boolean isValidId(Long id) {
        if (id > 0) {
            return true;
        } else {
            throw new ShipBadRequestException();
        }
    }

    private boolean isValidOrder(String name) {
        return !name.equals("");
    }

    private boolean isValidPageNumber(Integer number) {
        return number != Integer.MAX_VALUE;
    }

    private boolean isValidPageSize(Integer number) {
        return number != Integer.MAX_VALUE;
    }

    private boolean isValidName(String name) {
        return name != null && !name.equals("") && name.length() < 50;
    }

    private boolean isValidCrewSize(Integer crewSize) {
        return crewSize != null && crewSize > 0 && crewSize < 9999;
    }

    private boolean isValidSpeed(Double speed) {
        return speed != null && speed < 0.99d;
    }

    private boolean isValidProdDate(Long prodDate) {
        return prodDate != null && prodDate > YEAR2800.getTimeInMillis()
                && prodDate < YEAR3019.getTimeInMillis();
    }

    private boolean isValidRating(Double rating) {
        return rating != null && rating < Double.MAX_VALUE;
    }
}

