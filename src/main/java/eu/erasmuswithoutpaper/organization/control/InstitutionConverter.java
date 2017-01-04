package eu.erasmuswithoutpaper.organization.control;

import eu.erasmuswithoutpaper.api.architecture.StringWithOptionalLang;
import eu.erasmuswithoutpaper.api.institutions.InstitutionsResponse;
import eu.erasmuswithoutpaper.api.registry.OtherHeiId;
import eu.erasmuswithoutpaper.api.types.contact.Contact;
import static eu.erasmuswithoutpaper.common.control.ConverterHelper.convertFlexibleAddress;
import static eu.erasmuswithoutpaper.common.control.ConverterHelper.convertToHttpWithOptionalLang;
import static eu.erasmuswithoutpaper.common.control.ConverterHelper.convertToStringWithOptionalLang;
import eu.erasmuswithoutpaper.organization.entity.Coordinator;
import eu.erasmuswithoutpaper.organization.entity.IdentificationItem;
import eu.erasmuswithoutpaper.organization.entity.Institution;
import eu.erasmuswithoutpaper.organization.entity.OrganizationUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

public class InstitutionConverter {
    @PersistenceContext(unitName = "connector")
    EntityManager em;
    
    public List<InstitutionsResponse.Hei> convertToHei(List<Institution> institutionList) {
        return
            institutionList.stream().map((institution) -> {
                InstitutionsResponse.Hei hei = new InstitutionsResponse.Hei();

                hei.getContact().addAll(convertToContact(institution.getInstitutionId()));
                hei.getMobilityFactsheetUrl().addAll(convertToHttpWithOptionalLang(institution.getFactsheetUrl()));
                hei.getName().addAll(convertToStringWithOptionalLang(institution.getName()));
                hei.getOtherId().addAll(convertToOtherHeiId(institution.getOtherId()));
                hei.getOunitId().addAll(getOrganizationUnitIds(institution.getOrganizationUnits()));
                hei.getWebsiteUrl().addAll(convertToHttpWithOptionalLang(institution.getWebsiteUrl()));
                
                hei.setHeiId(institution.getInstitutionId());
                hei.setLogoUrl(institution.getLogoUrl());
                hei.setMailingAddress(convertFlexibleAddress(institution.getMailingAddress()));
                // TODO: set root ounit
                //hei.setRootOunitId();
                hei.setStreetAddress(convertFlexibleAddress(institution.getStreetAddress()));
                
                return hei;
            }).collect(Collectors.toList());
    }
    
    private List<OtherHeiId> convertToOtherHeiId(List<IdentificationItem> identificationItems) {
        return
            identificationItems.stream().map((identificationItem) -> {
                OtherHeiId otherHeiId = new OtherHeiId();
                otherHeiId.setType(identificationItem.getIdType());
                otherHeiId.setValue(identificationItem.getIdValue());
                return otherHeiId;
            }).collect(Collectors.toList());
    }
    
    private List<Contact> convertToContact(String institutionId) {
        Query query = em.createNamedQuery(Coordinator.findByInstWithNoOrgUnit).setParameter("institutionId", institutionId);
        List<Coordinator> coordinators = query.getResultList();

        return
            coordinators.stream().map((coordinator) -> {
                Contact contact  = new Contact();
                StringWithOptionalLang name = new StringWithOptionalLang();
                name.setValue(coordinator.getPerson().getFirstNames() + " " + coordinator.getPerson().getLastName());
                contact.getContactName().add(name);
                // TODO: add more information
                return contact;
            }).collect(Collectors.toList());
    }

    private List<String> getOrganizationUnitIds(List<OrganizationUnit> organizationUnits) {
        List<String> organizationUnitIds = new ArrayList<>();
        organizationUnits.stream().map((ou) -> {
            organizationUnitIds.add(ou.getOrganizationUnitId());
            return ou;
        }).forEachOrdered((ou) -> {
            organizationUnitIds.addAll(getOrganizationUnitIds(ou.getOrganizationUnits()));
        });
        return organizationUnitIds;
    }
}
